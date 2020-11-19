import java.math.BigInteger;
import java.util.Random;

public class Polynomial {
    BigInteger[] a;
    int degree = 2;

    /**
     * Ideally, we should be able to create n degree polynomial, but right now this
     * constructor only creates a 2 degree polynomial, ie. ax^2+bx+c.
     */
    Polynomial(BigInteger secret) {
        Random r = new Random();
        int nBits = 80;

        a = new BigInteger[degree + 1];

        a[0] = secret;
        a[1] = new BigInteger(nBits, r);
        a[2] = new BigInteger(nBits, r);
    }

    /**
     * Given an x, calculates and return f(x).
     *
     * @param x input x.
     * @return the value of f(x). Evaluation is done by plugging the value of x into
     * the polynomial representation.
     */
    public BigInteger f(int x) {
        BigInteger result = new BigInteger("0");
        for (int i = 0; i <= degree; i++) {
            //a[2]*x^2 + a[1]*x^1 + a[0].
            //The peril of using Java: having to write unreadable code for simple
            // things :(
            BigInteger multFactor = new BigInteger(String.valueOf((int) Math.pow(x, i)));
            result = result.add(a[i].multiply(multFactor));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public BigInteger calculateSecret2Degree(int[] x, BigInteger[] y) {
        BigInteger result;
        result =
                y[2].multiply(new BigInteger(String.valueOf(x[0] * x[1] * (x[0] - x[1]))))
                        .add(y[0].multiply(new BigInteger(String.valueOf(x[1] * x[2] * (x[1] - x[2])))))
                        .add(y[1].multiply(new BigInteger(String.valueOf(x[0] * x[2] * (x[2] - x[0])))));
        return result.divide(new BigInteger(String.valueOf(
                (x[2] * x[2]) - (x[2] * (x[1] + x[0])) + (x[0] * x[1]))).
                multiply(new BigInteger(String.valueOf(x[0] - x[1]))));
    }

    /**
     * In a t,n scheme, given t values for x and t values for y, this function returns
     * the value of f(0). This function is not generic and only works for a polynomial
     * of degree 2, ie. ax^2+bx+c.
     * The formula for calculating the secret is too complicated to be written in this
     * doc. Please see README.md for the formula.
     */
    public static BigInteger calculateSecret(int[] x, BigInteger[] y, int degree) {
        if (degree != 2){
            try {
                throw new UnsupportedPolynomialException();
            } catch (UnsupportedPolynomialException e) {
                e.printStackTrace();
                System.err.println("Trying to solve polynomial assuming degree 2...");
            }
        }
        Polynomial polynomial = new Polynomial(new BigInteger("0"));
        return polynomial.calculateSecret2Degree(x, y);
    }

    @Override
    public String toString() {
        return "f(x) = " +
                a[2] + "x^2 + " +
                a[1] + "x + " +
                a[0];
    }
}
