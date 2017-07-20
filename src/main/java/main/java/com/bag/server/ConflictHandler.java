package main.java.com.bag.server;

import main.java.com.bag.operations.DeleteOperation;
import main.java.com.bag.operations.IOperation;
import main.java.com.bag.operations.UpdateOperation;
import main.java.com.bag.server.database.interfaces.IDatabaseAccess;
import main.java.com.bag.util.Log;
import main.java.com.bag.util.storage.NodeStorage;
import main.java.com.bag.util.storage.RelationshipStorage;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * Receives read and write sets and checks them for conflicts
 */
public class ConflictHandler
{
    /**
     * Private standard constructor to hide the implicit one.
     */
    private ConflictHandler()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Checks for conflicts between read and writeSets.
     *
     * @param globalWriteSet      the node and relationship global writeSet.
     * @param latestWriteSet      the AbstractRecoverable.KEEP_LAST_X writes.
     * @param localWriteSet       the node and relationship write set of the transaction.
     * @param readSetNode         the node readSet.
     * @param readSetRelationship the relationship readSet
     * @param snapshotId          the snapShotId of the transaction.
     * @return true if no conflict has been found.
     */
    protected static boolean checkForConflict(
            ConcurrentSkipListMap<Long, List<IOperation>> globalWriteSet,
            Map<Long, List<IOperation>> latestWriteSet,
            List<IOperation> localWriteSet,
            List<NodeStorage> readSetNode,
            List<RelationshipStorage> readSetRelationship,
            long snapshotId,
            IDatabaseAccess access)
    {
        //Commented out during first experiments because implementation is buggy
        //TODO check this, is throwing ClassCastException...
        return isUpToDate(globalWriteSet, latestWriteSet, localWriteSet, readSetNode, readSetRelationship, snapshotId) && isCorrect(readSetNode, readSetRelationship, access);
    }

    /**
     * Checks if no changes have been made since the start of the transaction.
     *
     * @param writeSet            the node and relationship writeSet.
     * @param latestWriteSet      the node and relationship write set of the transaction.
     * @param localWriteSet       the node and relationship writeSet of the transaction.
     * @param readSetNode         the node readSet.
     * @param readSetRelationship the relationship readSet
     * @param snapshotId          the snapShotId of the transaction.
     * @return true if data is up to date.
     */
    private static boolean isUpToDate(
            ConcurrentSkipListMap<Long, List<IOperation>> writeSet, Map<Long, List<IOperation>> latestWriteSet, List<IOperation> localWriteSet,
            List<NodeStorage> readSetNode,
            List<RelationshipStorage> readSetRelationship, long snapshotId)
    {

        List<IOperation> pastWrites = new ArrayList<>();

        boolean commit = true;
        if (!readSetNode.isEmpty())
        {
            if (!writeSet.isEmpty() && snapshotId <= writeSet.lastKey())
            {

                pastWrites = writeSet.entrySet()
                        .stream()
                        .filter(id -> id.getKey() > snapshotId)
                        .filter(entry -> entry.getKey() > snapshotId)
                        .map(Map.Entry::getValue)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());

            }
            
            pastWrites.addAll(latestWriteSet.entrySet()
                    .stream()
                    .filter(id -> id.getKey() > snapshotId)
                    .filter(entry -> entry.getKey() > snapshotId)
                    .map(Map.Entry::getValue)
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));

            List<IOperation> copy = new ArrayList<>(pastWrites);
            commit = readSetNode.isEmpty() || !copy.removeAll(readSetNode);
        }

        if (!commit)
        {
            if (!localWriteSet.isEmpty())
            {
                Log.getLogger().info("Aborting because of writeSet containing node read");
            }
            return false;
        }

        if (!readSetRelationship.isEmpty())
        {
            if (pastWrites.isEmpty())
            {
                if (!writeSet.isEmpty() && snapshotId <= writeSet.lastKey())
                {
                    pastWrites = writeSet.entrySet()
                            .stream()
                            .filter(id -> id.getKey() > snapshotId)
                            .filter(entry -> entry.getKey() > snapshotId)
                            .map(Map.Entry::getValue)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

                }
                pastWrites.addAll(latestWriteSet.entrySet()
                        .stream()
                        .filter(id -> id.getKey() > snapshotId)
                        .filter(entry -> entry.getKey() > snapshotId)
                        .map(Map.Entry::getValue)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));

            }
            List<IOperation> copy = new ArrayList<>(pastWrites);
            commit = readSetRelationship.isEmpty() || !copy.removeAll(readSetRelationship);
        }

        if (!commit)
        {
            if (!localWriteSet.isEmpty())
            {
                Log.getLogger().info("Aborting because of writeSet containing rs read");
            }
            return false;
        }

        final List<IOperation> tempList = localWriteSet.stream().filter(operation -> operation instanceof DeleteOperation || operation instanceof UpdateOperation)
                .collect(Collectors.toList());

        if (!tempList.isEmpty())
        {
            if (pastWrites.isEmpty())
            {
                if (!writeSet.isEmpty() && snapshotId <= writeSet.lastKey())
                {
                    pastWrites = writeSet.entrySet()
                            .stream()
                            .filter(id -> id.getKey() > snapshotId)
                            .filter(entry -> entry.getKey() > snapshotId)
                            .map(Map.Entry::getValue)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());
                }

                pastWrites.addAll(latestWriteSet.entrySet()
                        .stream()
                        .filter(id -> id.getKey() > snapshotId)
                        .filter(entry -> entry.getKey() > snapshotId)
                        .map(Map.Entry::getValue)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));
            }
            List<IOperation> copy = new ArrayList<>(pastWrites);

            commit = tempList.isEmpty() || !copy.removeAll(tempList);
        }
        if (!commit && !localWriteSet.isEmpty())
        {
            Log.getLogger().info("Aborting because of writeSet containing clashing operation");
        }

        return commit;
    }

    /**
     * Checks if readData matches with data in database.
     *
     * @param readSetNode         the node readSet.
     * @param readSetRelationship the relationship readSet
     * @return true if correct.
     */
    private static boolean isCorrect(List<NodeStorage> readSetNode, List<RelationshipStorage> readSetRelationship, IDatabaseAccess access)
    {
        boolean eq = access.equalHash(readSetNode) && access.equalHash(readSetRelationship);
        if (!eq)
        {
            Log.getLogger().warn("Aborting because of incorrect read");
        }
        return eq;
    }
}
