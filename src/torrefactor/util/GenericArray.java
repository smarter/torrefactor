package torrefactor.util;

class GenericArray<T> {
    public final Object[] array;
    public final int length;

    public GenericArray (int size) {
        this.array = new Object[size];
        this.length = this.array.length;
    }

    public void put (int index, T object) {
        this.array[index] = (Object) object;
    }

    @SuppressWarnings(value = "unchecked")
    public T get (int index) {
        return (T) this.array[index];
    }
}
