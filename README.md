# Multi-Party-Computation
Demonstrations of the power of Multi Party Computation.

# Calculating the constant term in a polynomial
For a 2 degree polynomial <code>f(x)=ax<sup>2</sup>+bx+c</code>, in [Shamir's Secret Sharing method](https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing), the idea is to encode a secret at `f(0)`. Hence, in our formula, the secret is the value `c`.

Now, given three equations, we have 3 unknowns, `a, b, c`. Finding the unknown `c` is solving these equations for the unknowns. 

Say the three equations are:

<pre>
a<sub>2</sub>x<sub>0</sub><sup>2</sup>+a<sub>1</sub>x<sub>0</sub>+a<sub>0</sub>

a<sub>2</sub>x<sub>1</sub><sup>2</sup>+a<sub>1</sub>x<sub>1</sub>+a<sub>0</sub>

a<sub>2</sub>x<sub>2</sub><sup>2</sup>+a<sub>1</sub>x<sub>2</sub>+a<sub>0</sub>
</pre>

A little pen and paper gives the formula to find <code>a<sub>0</sub></code> as:

![Formula for finding the constant term in the polynomial](finding_c.jpg "Finding the constant term in the polynomial")
