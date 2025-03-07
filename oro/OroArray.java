package oro;

import java.util.ArrayList;
import java.util.List;

class OroArray {
    private final List<Object> elements;

    OroArray(List<Object> elements) {
        this.elements = new ArrayList<>(elements);
    }

    OroArray() {
        this.elements = new ArrayList<>();
    }

    Object get(int index) {
        if (index < 0 || index >= elements.size()) {
            return "OroError: Array index out of bounds: " + index;
        }
        return elements.get(index);
    }
    
    void set(int index, Object value) {
        if (index < 0 || index >= elements.size()) {
            System.out.println("OroError: Array index out of bounds: " + index);
            return;
        }
        elements.set(index, value);
    }

    void append(Object value) {
        elements.add(value);
    }

    Object remove(int index) {
        if (index < 0 || index >= elements.size()) {
            return "Array index out of bounds: " + index;
        }
        return elements.remove(index);
    }

    int size() {
        return elements.size();
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    // Helper method for index assignment in Interpreter
    public List<Object> getArray(){
        return elements;
    }
}
