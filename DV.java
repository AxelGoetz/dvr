import java.lang.Math;

public class DV implements RoutingAlgorithm {

    static int LOCAL = -1;
    static int UNKNOWN = -2;
    static int INFINITY = 60;

    private Router router;
    private int updateInterval;
    private boolean allowPReverse;
    private boolean allowExpire;

    private ArrayList<RoutingTableEntry> routingTable;

    public DV() {
        routingTable = new ArrayList<RoutingTableEntry>();
    }

    public void setRouterObject(Router obj) {
        this.router = obj;
    }

    public void setUpdateInterval(int u) {
        this.updateInterval = u;
    }

    public void setAllowPReverse(boolean flag) {
        this.allowPReverse = flag;
    }

    public void setAllowExpire(boolean flag) {
        this.allowExpire = flag;
    }

    public void initalise() {
        routingTable.add(new DVRoutingTableEntry(router.getId(), LOCAL, 0, router.getCurrentTime());
    }

    /**
     * For a given destination address, returns the appriopriate interface
     * to send the message to.
     * It does this by looping through the routingTable and if it finds an entry whose dest matches,
     * it returns the interface.
     * There is no need to check for local interfaces.
     * @return the local interface
     */
    public int getNextHop(int destination) {
        for(DVRoutingTableEntry entry: routingTable) {
            if(entry.getDestination() == destination) {
                if(entry.getMetric() == INFINITY) {
                    return UNKOWN;
                }
                return entry.getInterface();
            }
        }
        return UNKOWN;
    }

    /**
     * If a particular interface fails, this method updates the routing table
     * and sets all values with that interface to infinity.
     * @param interface the id of the interface
     */
    private void setInterfaceToInfinity(int interface) {
        for(DVRoutingTableEntry entry: routingTable) {
            if(entry.getInterface() == interface) {
                entry.setMetric(INFINITY);
            }
        }
    }

    public void tidyTable() {
        for(int i = 0; i < router.getNumInterfaces(); i++) {
            if(!router.getInterfaceState(i)) {
                setInterfaceToInfinity(i);
            }
        }

        // TODO: Expire
    }

    /**
     * Given an interface, generates the appripriate routing packet payload.
     * @param  interface
     */
    private Payload getPayLoadRoutingPacket(int interface) {
        Payload payload = new Payload();
        for(DVRoutingTableEntry entry: routingTable) {
            // TODO: allowPReverse
            payload.addEntry(entry);
        }
        return payload;
    }

    /**
     * If an interface is up, it generates a routing packet and sends it
     * @return routing packet or null if the interface is down
     */
    public Packet generateRoutingPacket(int iface) {
        int time = router.getCurrentTime();

        if(router.getInterfaceState(iface)) {
            Payload payload = getPayLoadRoutingPacket(iface);
            Packet packet = new RoutingPacket(router.getId(), Packet.BROADCAST);
            packet.setPayload(payload);

            return packet;
        }
        return null;
    }

    /**
     * Given a routing packet p that came in on interface iface,
     * it processes the packet and updates the routing table.
     */
    public void processRoutingPacket(Packet p, int iface) {
        // TODO: Fininsh this function
    }

    /**
     * Prints the routing table out in the provided format
     */
    public void showRoutes() {
        System.out.println("Router " + router.getId());
        for(RoutingTableEntry entry : routingTable) {
            System.out.println(entry.toString());
        }
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
        return "d " + this.getDestination() + " i " + this.getInterface() + " m " + this.getMetric();
    }
}
