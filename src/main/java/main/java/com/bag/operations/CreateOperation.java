package main.java.com.bag.operations;

import main.java.com.bag.server.database.interfaces.IDatabaseAccess;
import main.java.com.bag.util.Log;
import main.java.com.bag.util.NodeStorage;
import main.java.com.bag.util.RelationshipStorage;

import java.io.Serializable;

/**
 * Create command which may be sent to the database.
 */
public class CreateOperation<S extends Serializable> implements Operation, Serializable
{
    private final S storage;

    public CreateOperation(final S key)
    {
        this.storage = key;
    }

    @Override
    public void apply(final IDatabaseAccess access, long snapshotId)
    {
        if(storage instanceof NodeStorage)
        {
            access.applyCreate((NodeStorage) storage, snapshotId);
        }
        else if(storage instanceof RelationshipStorage)
        {
            access.applyCreate((RelationshipStorage) storage, snapshotId);
        }
        else
        {
            Log.getLogger().warn("Trying to create incorrect type in the database.");
        }
    }
}