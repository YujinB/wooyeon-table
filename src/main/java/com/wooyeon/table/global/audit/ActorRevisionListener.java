package com.wooyeon.table.global.audit;

import org.hibernate.envers.RevisionListener;

/** 변경 이력을 남길 때 지금 주체를 함께 기록한다 */
public class ActorRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevision) revisionEntity).setActor(ActorContext.current());
    }
}
