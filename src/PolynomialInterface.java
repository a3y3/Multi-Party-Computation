import java.math.BigInteger;

public interface PolynomialInterface {
    /**
     * Given an x, calculates and return f(x).
     * @param x input x.
     * @return the value of f(x). Evaluation is done by plugging the value of x into
     * the polynomial representation.
     */
    BigInteger f(int x);

    /**
     * In a t,n scheme, given t values for x and t values for y, this function returns
     * the value of f(0). This function is not generic and only works for a polynomial
     * of degree 2, ie. ax^2+bx+c.
     * The formula for calculating the secret is too complicated to be written in this
     * doc. Please see README.md for the formula.
     */
    BigInteger calculateSecret2Degree(int[] x, BigInteger[] y);
}
