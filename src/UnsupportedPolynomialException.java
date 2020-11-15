public class UnsupportedPolynomialException extends Exception {
    public UnsupportedPolynomialException() {
        super();
        System.err.println("Currently, only polynomials of degree 2 are supported");
    }
}
