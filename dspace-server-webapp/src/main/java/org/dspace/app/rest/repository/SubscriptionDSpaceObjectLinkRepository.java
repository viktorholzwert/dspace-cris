/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.apache.poi.poifs.crypt.DataSpaceMapUtils;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.SQLException;

/**
 * Link repository for "mappedCollections" subresource of an individual item.
 */
@Component(SubscriptionRest.CATEGORY + "." + SubscriptionRest.NAME + "." + SubscriptionRest.DSPACE_OBJECT)
@Transactional
public class SubscriptionDSpaceObjectLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    SubscribeService subscribeService;

    @Autowired
    ItemService itemService;

    public DSpaceObjectRest getDSpaceObject(@Nullable HttpServletRequest request,
                                            Integer subscriptionId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) {
        try {
            Context context = obtainContext();
            Subscription subscription = subscribeService.findById(context, subscriptionId);
            if (subscription == null) {
                throw new ResourceNotFoundException("No such subscription: " + subscriptionId);
            }
            HibernateProxy hibernateProxy = (HibernateProxy) subscription.getdSpaceObject();
            LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();

            return converter.toRest(initializer.getImplementation(), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}