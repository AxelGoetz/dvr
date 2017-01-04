import java.lang.Math;

public class DV implements RoutingAlgorithm {

    static int LOCAL = -1;
    static int UNKNOWN = -2;
    static int INFINITY = 60;

    public DV() {
    }

    public void setRouterObject(Router obj) {
    }

    public void setUpdateInterval(int u) {
    }

    public void setAllowPReverse(boolean flag) {
    }

    public void setAllowExpire(boolean flag) {
    }

    public void initalise() {
    }

    public int getNextHop(int destination) {
        return 0;
    }

    public void tidyTable() {
    }

    public Packet generateRoutingPacket(int iface) {
        return null;
    }

    public void processRoutingPacket(Packet p, int iface) {
    }

    public void showRoutes() {
    }
}

class DVRoutingTableEntry implements RoutingTableEntry {
    private int destination;
    private int interface;
    private int metric;
    private int time;

    public DVRoutingTableEntry(int d, int i, int m, int t) {
        this.destination = d;
        this.interface = i;
        this.metric = m;
        this.time = t;
    }

    public int getDestination() { return this.destination; }

    public void setDestination(int d) { this.destination = d; }

    public int getInterface() { return this.interface; }

    public void setInterface(int i) { this.interface = i; }

    public int getMetric() { return this.metric; }

    public void setMetric(int m) {this.metric = m }

    public int getTime() { return this.time; }

    public void setTime(int t) { this.time = t; }

    public String toString() {
        return "d " + this.destination + " i " + this.interface + " m " + this.metric;
    }
}
