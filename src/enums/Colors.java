package enums;

public enum Colors {
    RED("\u001B[41m"),
    GREEN("\u001B[42m"),
    YELLOW("\u001B[43m"),
    BRIGHT_DARK("\u001B[40m"),
    RESET("\u001B[0m");
    private final String color;
    Colors(String color){
        this.color = color;
    }

    @Override
    public String toString() {
        return color;
    }
}
