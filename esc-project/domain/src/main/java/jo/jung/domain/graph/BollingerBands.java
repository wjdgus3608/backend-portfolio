package jo.jung.domain.graph;

public class BollingerBands {
    private final double upper;
    private final double middle;
    private final double lower;

    public BollingerBands(double upper, double middle, double lower) {
        this.upper = upper;
        this.middle = middle;
        this.lower = lower;
    }

    public double getUpper() {
        return upper;
    }

    public double getMiddle() {
        return middle;
    }

    public double getLower() {
        return lower;
    }

    @Override
    public String toString() {
        return "BollingerBands{" +
                "upper=" + upper +
                ", middle=" + middle +
                ", lower=" + lower +
                '}';
    }

    public boolean isTooSmall(long price){
        long tick = getTick(price);
        return getUpper()-getLower() <= tick * 4f;
    }

    public int getTick(long pricePerStock){
        if(pricePerStock < 2000)
            return 1;
        else if(pricePerStock < 5000)
            return 5;
        else if(pricePerStock < 20000)
            return 10;
        else if(pricePerStock < 50000)
            return 50;
        else if(pricePerStock < 200000)
            return 100;
        else if(pricePerStock < 500000)
            return 500;
        else
            return 1000;
    }
}
