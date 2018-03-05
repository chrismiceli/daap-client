///Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL
package javax.jmdns.impl;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

/** ServiceEvent.
 * @author Werner Randelshofer, Rick Blair
 * @version %I%, %G% */
@SuppressWarnings("serial")
public class ServiceEventImpl extends ServiceEvent {
    /** The type name of the service. */
    private String type;
    /** The instance name of the service. Or null, if the event was fired to a
     * service type listener. */
    private String name;
    /** The service info record, or null if the service could be be resolved.
     * This is also null, if the event was fired to a service type listener. */
    private ServiceInfoImpl info;

    /** Creates a new instance.
     * @param source
     * the JmDNS instance which originated the event.
     * @param type
     * the type name of the service.
     * @param name
     * the instance name of the service.
     * @param info
     * the service info record, or null if the service could be be
     * resolved. */
    public ServiceEventImpl(JmDNSImpl source, String type, String name,
            ServiceInfoImpl info) {
        super(source);
        this.type = type;
        this.name = name;
        this.info = info;
    }

    /** @see javax.jmdns.ServiceEvent#getDNS() */
    public JmDNS getDNS() {
        return (JmDNS) getSource();
    }

    /** @see javax.jmdns.ServiceEvent#getType() */
    public String getType() {
        return type;
    }

    /** @see javax.jmdns.ServiceEvent#getName() */
    public String getName() {
        return name;
    }

    /** @see javax.jmdns.ServiceEvent#toString() */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("<" + getClass().getName() + "> ");
        buf.append(super.toString());
        buf.append(" name ");
        buf.append(getName());
        buf.append(" type ");
        buf.append(getType());
        buf.append(" info ");
        buf.append(getInfo());
        return buf.toString();
    }

    public ServiceInfo getInfo() {
        return info;
    }
}