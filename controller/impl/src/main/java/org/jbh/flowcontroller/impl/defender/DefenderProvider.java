/*
 * Copyright © 2017 liuhy and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.jbh.flowcontroller.impl.defender;


import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefenderProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DefenderProvider.class);

    private final DataBroker dataBroker;
    private PacketHandler packetHandler;

    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;

    private ListenerRegistration<NotificationListener> registration = null;

    public DefenderProvider(final DataBroker dataBroker,
                            final NotificationPublishService notificationPublishService,
                            final NotificationService notificationService ) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("DefenderProvider Session Initiated");
        if (notificationService != null)
        {
            LOG.info("NotificationService is: " + notificationService.toString());
            packetHandler = new PacketHandler();
            registration = notificationService.registerNotificationListener(packetHandler);
        }
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close()
    {
        LOG.info("DefenderProvider Closed");
        packetHandler.closeFileWriter();
        if( registration != null)
        {
            registration.close();
        }
    }
}