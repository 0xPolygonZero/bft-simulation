/**
 * A three-dimensional vector of doubles.
 */
class Vector3d {
  private final double x, y, z;

  Vector3d(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /** The Euclidean norm of this vector. */
  double norm() {
    return Math.sqrt(x * x + y * y + z * z);
  }

  Vector3d scaled(double s) {
    return new Vector3d(s * x, s * y, s * z);
  }

  Vector3d normalized() {
    return scaled(1 / norm());
  }

  double dotProduct(Vector3d that) {
    return this.x * that.x + this.y * that.y + this.z * that.z;
  }

  @Override public String toString() {
    return String.format("(%f, %f, %f)", x, y, z);
  }
}
