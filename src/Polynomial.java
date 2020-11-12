import java.math.BigInteger;

public class Polynomial implements PolynomialInterface {
    BigInteger[] a;
    int[] x;
    int degree = 2;

    /**
     * Ideally, we should be able to create n degree polynomial, but right now this
     * constructor only creates a 2 degree polynomial, ie. ax^2+bx+c.
     */
    Polynomial(BigInteger secret) {
        a = new BigInteger[degree + 1];
        x = new int[degree + 1];

        a[0] = secret;
        a[1] = new BigInteger("20");
        a[2] = new BigInteger("10");

        x[degree] = 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger f(int x) {
        BigInteger result = new BigInteger("0");
        for (int i = 0; i <= degree; i++) {
            //a[2]*x^2 + a[1]*x^1 + a[0].
            //The peril of using Java: having to write unreadable code for simple
            // things :(
            BigInteger multFactor = new BigInteger(String.valueOf((int)Math.pow(x, i)));
            result = result.add(a[i].multiply(multFactor));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger calculateSecret2Degree(int[] x, BigInteger[] y) {
        BigInteger result;
        result =
                y[2].multiply(new BigInteger(String.valueOf(x[0] * x[1] * (x[0] - x[1]))))
                        .add(y[0].multiply(new BigInteger(String.valueOf(x[1] * x[2] * (x[1] - x[2])))))
                        .add(y[1].multiply(new BigInteger(String.valueOf(x[0] * x[2] * (x[2] - x[0])))));
        return result.divide(new BigInteger(String.valueOf(
                (x[2] * x[2]) - x[2] * (x[1] + x[0]) + x[0] * x[1])).
                multiply(new BigInteger(String.valueOf(x[0] - x[1]))));
    }

    public static BigInteger calculateSecret(int[] x, BigInteger[] y, int degree) {
        Polynomial polynomial = new Polynomial(new BigInteger("0"));
        return polynomial.calculateSecret2Degree(x, y);
    }
}
