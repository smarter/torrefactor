package torrefactor.util;

/**
 * A simple array wich allow to use safely generic types
 */
class GenericArray<T> {
    private final Object[] array;

    /**
     * The length of the array.
     */
    public final int length;

    /**
     * Create a new array.
     *
     * @param size  the size of the array
     */
    public GenericArray (int size) {
        this.array = new Object[size];
        this.length = this.array.length;
    }

    /**
     * Put an element in the array.
     *
     * @param index  the index where to but the object
     * @param object the object to put in the array
     */
    public void put (int index, T object) {
        this.array[index] = (Object) object;
    }

    /**
     * Get an element from the array.
     *
     * @param index the index of the object to return
     * @return the element at the given position
     */
    @SuppressWarnings(value = "unchecked")
    public T get (int index) {
        return (T) this.array[index];
    }
}
