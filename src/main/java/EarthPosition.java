import java.util.Random;

/**
 * A position on the earth.
 */
class EarthPosition {
  /** The Earth's radius, in meters. */
  private static final double EARTH_RADIUS = 6.378e6;

  /** A normalized vector representing the direction of this position from the Earth's center. */
  private final Vector3d direction;

  static EarthPosition randomPosition(Random r) {
    // Generate random points in the bounding box of the unit sphere, but skip points which lie
    // outside the unit sphere. This gives us random points uniformly distributed inside the unit
    // sphere, which we then normalize to get random points uniformly distributed on the surface.
    while (true) {
      double x = nextDouble(r, -1, 1),
          y = nextDouble(r, -1, 1),
          z = nextDouble(r, -1, 1);
      Vector3d point = new Vector3d(x, y, z);
      if (point.norm() <= 1) {
        return new EarthPosition(point.normalized());
      }
    }
  }

  private EarthPosition(Vector3d direction) {
    this.direction = direction;
  }

  /** The great-circle distance to another earth position, in meters. */
  double getDistance(EarthPosition that) {
    double product = this.direction.dotProduct(that.direction);

    // Although both direction vectors are normalized, their dot product may be slightly outside of
    // [-1, 1] due to floating point error. Clamp to [-1, 1] to make sure.
    product = Math.max(product, -1);
    product = Math.min(product, 1);

    double centralAngle = Math.acos(product);
    if (Double.isNaN(centralAngle)) {
      throw new AssertionError("acos returned NaN");
    }
    return EARTH_RADIUS * centralAngle;
  }

  private static double nextDouble(Random r, double min, double max) {
    return min + r.nextDouble() * (max - min);
  }
}
