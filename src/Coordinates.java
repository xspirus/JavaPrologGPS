public class Coordinates {

    private Double x, y;

    Coordinates(Double x, Double y) {
        this.x = x;
        this.y = y;
    }

    Coordinates(Coordinates other) {
        this.x = other.x;
        this.y = other.y;
    }

    public Double getX() {
        return this.x;
    }

    public Double getY() {
        return this.y;
    }

    /**
     * Calculates the distance between two Earth coordinates.
     * @param x The other x.
     * @param y The other y.
     * @return The distance in KiloMetres.
     */
    public Double distance(Double x, Double y) {
        return this.distance(new Coordinates(x, y));
    }

    /**
     * Calculates the distance between two Earth coordinates.
     * @param other The other coordinates.
     * @return The distance in KiloMetres.
     */
    public Double distance(Coordinates other) {
        Double r = 6371.0;
        Double dLat = degreesToRadians(this.y - other.y);
        Double dLon = degreesToRadians(this.x - other.x);
        Double lat1 = degreesToRadians(this.y);
        Double lat2 = degreesToRadians(other.y);
        Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (r * c);
    }

    private Double degreesToRadians(double degrees) {
        return (degrees * Math.PI / 180);
    }

    /**
     * The equals method. Two coordinates are equal
     * if they have the same x and y.
     * @param o The other coordinates.
     * @return true iff same x && y.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Coordinates))
            return false;

        Coordinates other = (Coordinates)o;
        if (this == other)
            return true;

        return (this.x.equals(other.x) && this.y.equals(other.y));
    }

    /**
     * General hash code.
     * @return int PseudoRandom.
     */
    @Override
    public int hashCode() {
        return (Double.valueOf(x).hashCode() * 31 + Double.valueOf(y).hashCode());
    }

    /**
     * The toString method. Used by kml
     * to println int the kml file.
     * @return String as "x, y".
     */
    @Override
    public String toString() {
        return (x + ", " + y);
    }

}
