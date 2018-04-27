import java.util.*;

/**
 * My implementation of a fixed size priority queue.
 * It is based on the <code>TreeSet</code> of JAVA.
 * However because <code>TreeSet</code> does not use
 * <code>equals</code> to remove certain objects, we
 * use an auxiliary <code>HashSet</code> with all the
 * elements that the <code>TreeSet</code> contains.
 * Whenever we need to remove an object we first remove
 * it from the <code>HashSet</code> and then put all of
 * its objects in the <code>TreeSet</code>, thus
 * maintaining the correct order of the objects.
 * @param <T> The type of objects in the priority queue.
 * @author Spiros Dontas
 */
public class PrioQueue<T> extends TreeSet<T> {

    private final int   capacity;
    private Set<T>      openSet;

    /**
     * Typical Constructor.
     * Initialises capacity and the <code>comparator</code>
     * of the <code>TreeSet</code>.
     * @param capacity The capacity of the queue
     * @param comparator The comparator to sort the objects.
     */
    public PrioQueue(int capacity, Comparator<T> comparator) {
        super(comparator);
        this.capacity = capacity;
        openSet = new HashSet<>();
    }

    /**
     * The add method.
     * Overrides the add method of <code>TreeSet</code>.
     * Adds the object to the hash set and to the tree set.
     * Checks if capacity has been reached and removes the last
     * element if so.
     * @param t The object to be added.
     * @return true iff the object was truly added successfully.
     */
    @Override
    public boolean add(T t) {
        if (openSet.add(t) && super.add(t)) {
            if (this.size() > capacity) {
                T removed = this.pollLast();
                openSet.remove(removed);
                if (removed.equals(t))
                    return false;
                return true;
            }
            return true;
        }
        return false;
    }

    /**
     * The pollFirst method.
     * Overrides the pollFirst method of <code>TreeSet</code>.
     * Removes the first element from the <code>TreeSet</code>,
     * as well as from the <code>HashSet</code>.
     * @return The element removed. {Default: null}.
     */
    @Override
    public T pollFirst() {
        T temp = super.pollFirst();
        if (openSet.remove(temp))
            return temp;
        return null;
    }

    /**
     * The pollLast method.
     * Overrides the pollLast method of <code>TreeSet</code>.
     * Removes the last element from the <code>TreeSet</code>,
     * as well as from the <code>HashSet</code>.
     * @return The element removed. {Default: null}.
     */
    @Override
    public T pollLast() {
        T temp = super.pollLast();
        if (openSet.remove(temp))
            return temp;
        return null;
    }

    /**
     * The remove method.
     * Overrides the remove method of <code>TreeSet</code>.
     * Removes the certain object from the <code>HashSet</code>
     * first, clears the <code>TreeSet</code> and reconstructs
     * it by adding all of the elements of the <code>HashSet</code>.
     * @param o The object to be removed.
     * @return true iff the object was removed successfully.
     */
    @Override
    public boolean remove(Object o) {
        T other = (T)o;
        if (openSet.remove(other)) {
            super.clear();
            for (T current : openSet)
                super.add(current);
            return true;
        }
        return false;
    }

    /**
     * The contains method.
     * Overrides the contains method of <code>TreeSet</code>.
     * Because the super class is based on comparing objects
     * with <code>compareTo</code> and not <code>equals</code>,
     * the <code>contains</code> method of the <code>HashSet</code>
     * is used instead.
     * @param o The object to be checked.
     * @return true iff the object is in the queue.
     */
    @Override
    public boolean contains(Object o) {
        return openSet.contains(o);
    }

    /**
     * The clear method.
     * Overrides the clear method of <code>TreeSet</code>.
     * Clears the <code>TreeSet</code> as well as the
     * <code>HashSet</code>.
     */
    @Override
    public void clear() {
        super.clear();
        openSet.clear();
    }

}
