package org.radarcns.management.domain.support;

import org.radarcns.management.domain.AbstractAuditingEntity;
import org.radarcns.management.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * EntityListener that publishes audit events to the ApplicationEventPublisher so we also have
 * separate audit logs for these events instead of only having the latest modified at and modified
 * by information. We can not autowire Spring beans into JPA classes, so we need to make use of
 * an AutowireHelper (source:
 * https://guylabs.ch/2014/02/22/autowiring-pring-beans-in-hibernate-jpa-entity-listeners/).
 */
@Component
public class EventPublisherEntityListener {

    public static final String ENTITY_CREATED = "ENTITY_CREATED";
    public static final String ENTITY_UPDATED = "ENTITY_UPDATED";
    public static final String ENTITY_REMOVED = "ENTITY_REMOVED";
    @Autowired
    private AuditEventRepository auditEventRepository;

    private DateTimeProvider dateTimeProvider = CurrentDateTimeProvider.INSTANCE;

    @PostPersist
    public void publishPersistEvent(AbstractAuditingEntity entity) {
        AutowireHelper.autowire(this, auditEventRepository);
        AuditEvent event = new AuditEvent(entity.getCreatedBy(), ENTITY_CREATED,
                createData(entity));
        auditEventRepository.add(event);
    }

    @PostUpdate
    public void publishUpdateEvent(AbstractAuditingEntity entity) {
        AutowireHelper.autowire(this, auditEventRepository);
        AuditEvent event = new AuditEvent(entity.getLastModifiedBy(), ENTITY_UPDATED,
                createData(entity));
        auditEventRepository.add(event);
    }

    @PostRemove
    public void publishRemoveEvent(AbstractAuditingEntity entity) {
        AutowireHelper.autowire(this, auditEventRepository);
        AuditEvent event = new AuditEvent(SecurityUtils.getCurrentUserLogin(), ENTITY_REMOVED,
                createData(entity));
        auditEventRepository.add(event);
    }

    /**
     * Get a standard description of the entity (class and toString() result) as well as the date
     * when the operation took place.
     * @param entity The entity for which to generate the description
     * @return a Map containing the description of the entity and date the operation took place
     */
    private Map<String, Object> createData(AbstractAuditingEntity entity) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityClass", entity.getClass().getName());
        data.put("entity", entity.toString());
        data.put("date", ZonedDateTime.ofInstant(dateTimeProvider.getNow().toInstant(),
                ZoneId.systemDefault()));
        return data;
    }
}
