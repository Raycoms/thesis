package main.java.com.bag.util.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Signature storage class. Used to store an amount of signatures.
 */
public class SignatureStorage implements Serializable
{
    /**
     * The message which has been signed with the signatures.
     */
    private byte[] message;

    /**
     * The stored signatures.
     */
    private final TreeMap<Integer, byte[]> signatures = new TreeMap<>();

    /**
     * The required amount to prove consistency.
     */
    private int requiredSignatures;

    /**
     * The decision of the message.
     */
    private String decision;

    /**
     * Is the signatureStorage processed by the holding replica.
     */
    private boolean isProcessed;

    /**
     * Standard constructor for kryo.
     */
    public SignatureStorage()
    {
        /*
         * Standard constructor empty on purpose.
         */
    }

    /**
     * Public constructor to create the storage.
     * @param requiredSignatures the amount which are required.
     */
    public SignatureStorage(final int requiredSignatures, final byte[] message, final String decision)
    {
        this.requiredSignatures = requiredSignatures;
        this.message = message;
        this.decision = decision;
    }

    /**
     * Create a SignatureStorage out of another one.
     * (Copy constructor)
     * @param signatureStorage signatureStorage to use.
     */
    public SignatureStorage(final SignatureStorage signatureStorage)
    {
        this.requiredSignatures = signatureStorage.requiredSignatures;
        this.message = signatureStorage.message;
        this.decision = signatureStorage.decision;
        this.signatures.putAll(signatureStorage.signatures);
    }

    /**
     * Add a signature to the storage.
     * @param globalId the id of the server in the global cluster.
     * @param signature the signature to add.
     */
    public void addSignatures(final int globalId, final byte[] signature)
    {
        signatures.put(globalId, signature);
    }

    /**
     * Check if there are enough signatures stored to prove consistency.
     * @return true if the requiredAmount has been stored.
     */
    public boolean hasEnough()
    {
        return signatures.size() >= requiredSignatures;
    }

    /**
     * Getter for the message.
     * @return byte[] of the message.
     */
    public byte[] getMessage()
    {
        return this.message;
    }

    /**
     * Getter of the signatures.
     * @return the copy of the signatures list.
     */
    public Map<Integer, byte[]> getSignatures()
    {
        return new TreeMap<>(signatures);
    }

    /**
     * Getter for the decision.
     * @return the string of the decision.
     */
    public String getDecision()
    {
        return this.decision;
    }

    /**
     * Set the message of the signature storage.
     * @param message to set.
     */
    public void setMessage(final byte[] message)
    {
        this.message = message;
    }

    /**
     * Set that the replica holding this storage object processed the commit.
     */
    public void setProcessed()
    {
        this.isProcessed = true;
    }

    /**
     * Check if the signatureStorage has been processed by the owning replica.
     * @return true if so.
     */
    public boolean isProcessed()
    {
        return isProcessed;
    }
}
