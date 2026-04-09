package com.hegi64.iceBoatTimeTrial.model;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public record RegionBox(
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        Double dirX,
        Double dirY,
        Double dirZ,
        Double directionThreshold
) {
    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean directionMatches(Vector movement, boolean enforceDirection, double defaultThreshold) {
        if (!enforceDirection) {
            return true;
        }
        if (dirX == null || dirY == null || dirZ == null) {
            return true;
        }
        if (movement.lengthSquared() < 1.0E-9) {
            return false;
        }
        Vector normal = new Vector(dirX, dirY, dirZ);
        if (normal.lengthSquared() < 1.0E-9) {
            return true;
        }
        Vector normalizedMove = movement.clone().normalize();
        Vector normalizedNormal = normal.normalize();
        double threshold = directionThreshold != null ? directionThreshold : defaultThreshold;
        return normalizedMove.dot(normalizedNormal) >= threshold;
    }

    public double centerDistanceSquared(Location location) {
        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double cz = (minZ + maxZ) * 0.5;
        return location.distanceSquared(new Location(location.getWorld(), cx, cy, cz));
    }
}

