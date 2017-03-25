package main.java.com.bag.evaluations;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import main.java.com.bag.client.TestClient;
import main.java.com.bag.operations.CreateOperation;
import main.java.com.bag.operations.DeleteOperation;
import main.java.com.bag.operations.UpdateOperation;
import main.java.com.bag.util.Log;
import main.java.com.bag.util.storage.NodeStorage;
import main.java.com.bag.util.storage.RelationshipStorage;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class containing the threads to simulate concurrent clients.
 */
public class ClientThreads
{
    /**
     * Location of the testGraph.
     */
    private static final String GRAPH_LOCATION = "/home/ray/IdeaProjects/BAG - Byzantine fault-tolerant Architecture for Graph database/src/testGraphs/social-a-graph.txt";

    private ClientThreads()
    {
        /*
         * Intentionally left empty.
         */
    }

    public static class MassiveNodeInsertThread implements Runnable
    {
        private TestClient client = null;
        private DataOutputStream out = null;

        private final int        startAt;
        private final int        stopAt;
        private final int        commitAfter;

        /**
         * Create a threadsafe version of kryo.
         */
        public KryoFactory factory = () ->
        {
            Kryo kryo = new Kryo();
            kryo.register(NodeStorage.class, 100);
            kryo.register(RelationshipStorage.class, 200);
            kryo.register(CreateOperation.class, 250);
            kryo.register(DeleteOperation.class, 300);
            kryo.register(UpdateOperation.class, 350);
            return kryo;
        };

        public MassiveNodeInsertThread(@NotNull final TestClient client, final int share, final int start, final int commitAfter, final int size)
        {
            this.client = client;
            startAt = start * (size/share) + 1;
            stopAt = startAt + (size/share) - 1;
            this.commitAfter = commitAfter;
        }

        public MassiveNodeInsertThread(final DataOutputStream out, final int share, final int start, final int commitAfter, final int size)
        {
            this.out = out;
            startAt = start * (size/share) + 1;
            stopAt = startAt + (size/share) - 1;
            this.commitAfter = commitAfter;
        }

        @Override
        public void run()
        {
            final KryoPool pool = new KryoPool.Builder(factory).softReferences().build();
            final Kryo kryo = pool.borrow();

            int written = 0;
            for (int i = startAt; i <= stopAt; i++)
            {
                written++;
                if(client == null)
                {
                    List<NodeStorage> nodeStorages = new ArrayList<>();
                    nodeStorages.add(new NodeStorage(Integer.toString(i)));

                    if (written >= commitAfter || i == stopAt)
                    {
                        try(final Output output = new Output(0, 10024))
                        {
                            for(NodeStorage nodeStorage : nodeStorages)
                            {
                                kryo.writeObject(output, nodeStorage);
                            }
                            out.write(output.getBuffer());
                        }
                        catch (IOException e)
                        {
                            Log.getLogger().warn("Exception while writing to the server.", e);
                        }
                        nodeStorages = new ArrayList<>();
                    }
                }
                else
                {
                    client.write(null, new NodeStorage(Integer.toString(i)));
                    if (written >= commitAfter || i == stopAt)
                    {
                        client.commit();
                    }
                }
            }
            pool.release(kryo);
        }
    }

    public static class MassiveRelationShipInsertThread implements Runnable
    {
        private TestClient client = null;
        private DataOutputStream out = null;

        private final int commitAfter;
        private final int share;
        private final int start;

        /**
         * Create a threadsafe version of kryo.
         */
        public KryoFactory factory = () ->
        {
            Kryo kryo = new Kryo();
            kryo.register(NodeStorage.class, 100);
            kryo.register(RelationshipStorage.class, 200);
            kryo.register(CreateOperation.class, 250);
            kryo.register(DeleteOperation.class, 300);
            kryo.register(UpdateOperation.class, 350);
            return kryo;
        };

        public MassiveRelationShipInsertThread(@NotNull final TestClient client, final int share, final int start, final int commitAfter)
        {
            this.client = client;
            this.share = share;
            this.start = start;
            this.commitAfter = commitAfter;
        }

        public MassiveRelationShipInsertThread(final DataOutputStream out, final int share, final int start, final int commitAfter)
        {
            this.out = out;
            this.share = share;
            this.start = start;
            this.commitAfter = commitAfter;
        }

        @Override
        public void run()
        {
            final KryoPool pool = new KryoPool.Builder(factory).softReferences().build();
            final Kryo kryo = pool.borrow();

            try(FileReader fr = new FileReader(GRAPH_LOCATION); BufferedReader br = new BufferedReader(fr);)
            {
                final long size = br.lines().count();
                final long totalShare = size / share;
                final long startAt = start * totalShare + 1;
                br.skip(startAt - 1);

                int readLines = 0;
                int writtenLines = 0;
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null)
                {
                    final String[] ids = sCurrentLine.split(" ");

                    if(ids.length != 3)
                    {
                        continue;
                    }

                    readLines++;
                    writtenLines++;

                    if(client == null)
                    {
                        List<RelationshipStorage> relationshipStorages = new ArrayList<>();
                        relationshipStorages.add(new RelationshipStorage(ids[1], new NodeStorage(ids[0]), new NodeStorage(ids[2])));

                        if (readLines >= totalShare)
                        {
                            try (final Output output = new Output(0, 10024))
                            {
                                for (RelationshipStorage relationshipStorage : relationshipStorages)
                                {
                                    kryo.writeObject(output, relationshipStorage);
                                }
                                out.write(output.getBuffer());
                            }
                            catch (IOException e)
                            {
                                Log.getLogger().warn("Exception while writing to the server.", e);
                            }
                            break;
                        }

                        if (writtenLines >= commitAfter)
                        {
                            try (final Output output = new Output(0, 10024))
                            {
                                for (RelationshipStorage relationshipStorage : relationshipStorages)
                                {
                                    kryo.writeObject(output, relationshipStorage);
                                }
                                out.write(output.getBuffer());
                            }
                            catch (IOException e)
                            {
                                Log.getLogger().warn("Exception while writing to the server.", e);
                            }
                            relationshipStorages = new ArrayList<>();
                        }
                    }
                    else
                    {
                        client.write(null, new RelationshipStorage(ids[1], new NodeStorage(ids[0]), new NodeStorage(ids[2])));
                        if (readLines >= totalShare)
                        {
                            client.commit();
                            break;
                        }

                        if (writtenLines >= commitAfter)
                        {
                            client.commit();
                        }
                    }
                    Log.getLogger().info(sCurrentLine);
                }
            }
            catch (IOException e)
            {
                Log.getLogger().warn("Error reading file", e);
            }
            finally
            {
                pool.release(kryo);
            }
        }
    }
}
