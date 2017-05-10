package main.java.com.bag.util.storage;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.Map;

/**
 * Class used to store Nodes locally for a limited amount of time.
 */
public class NodeStorage implements Serializable
{
    /**
     * The string identifier of the node.
     */
    @NotNull
    private final String id;

    /**
     * The properties of the node, may be empty as well.
     */
    @NotNull
    private Map<String, Object> properties = new TreeMap<>();

    public NodeStorage()
    {
        id = "Node";
    }

    /**
     * Simple nodeStorage constructor.
     *
     * @param id string identifier of the node.
     */
    public NodeStorage(@NotNull String id)
    {
        this.id = "Node";
        this.properties = new TreeMap<>();
        this.properties.put("idx", id);
    }

    /**
     * Simple nodeStorage constructor.
     *
     * @param id         string identifier of the node.
     * @param properties properties of the node.
     */
    public NodeStorage(@NotNull String id, @NotNull Map<String, Object> properties)
    {
        this.id = "Node";
        this.properties.putAll(properties);
        if (!properties.containsKey("idx"))
        {
            this.properties.put("idx", id);
        }
    }

    /**
     * Getter of the id.
     *
     * @return string description of the node.
     */
    @NotNull
    public String getId()
    {
        return this.id;
    }

    /**
     * Getter of the properties.
     *
     * @return unmodifiable map of the properties.
     */
    @NotNull
    public Map<String, Object> getProperties()
    {
        return new TreeMap<>(properties);
    }

    /**
     * Sets or adds new properties.
     *
     * @param properties a property map.
     */
    public void setProperties(@NotNull final Map<String, Object> properties)
    {
        this.properties.putAll(properties);
    }

    /**
     * Add a new property to the properties.
     *
     * @param description description of the property.
     * @param value       value of the property.
     */
    public void addProperty(String description, Object value)
    {
        this.properties.put(description, value);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final NodeStorage that = (NodeStorage) o;

        //We always have some type of type/class/name
        if (!this.getId().equals(that.getId()) && !this.getId().isEmpty() && !that.getId().isEmpty())
        {
            return false;
        }
        //todo is the same if valid subset in whatEver direction.
        return this.getProperties().entrySet().containsAll(that.getProperties().entrySet())
                || that.getProperties().entrySet().containsAll(this.getProperties().entrySet());
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * getId().hashCode() + getProperties().hashCode());
    }

    /**
     * Remove a certain property from the properties.
     *
     * @param key the key of the property which should be removed.
     */
    public void removeProperty(String key)
    {
        properties.remove(key);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(id);

        sb.append("[");
        for (Map.Entry<String, Object> item : properties.entrySet())
        {
            if(item.getKey().equals("hash") || item.getKey().equals("snapShotId"))
            {
                continue;
            }
            sb.append(item.getKey());
            sb.append("=");
            sb.append(item.getValue());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");


        return sb.toString();
    }

    /**
     * Returns a byte representation of the nodeStorage.
     *
     * @return a byte array.
     */
    public byte[] getBytes()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(id);

        for (Map.Entry<String, Object> entry : properties.entrySet())
        {
            sb.append(entry.getKey()).append(entry.getValue());
        }

        return sb.toString().getBytes();
    }
}
