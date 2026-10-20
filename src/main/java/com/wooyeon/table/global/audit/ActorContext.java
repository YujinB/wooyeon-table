package com.wooyeon.table.global.audit;

/**
 * 지금 요청을 처리하는 주체. 상태 변경 이력에 "누가" 바꿨는지 남기는 데 쓴다.
 * 로그인 요청은 인증 인터셉터가 채우고, 스케줄러·서버 시작 작업처럼 요청 밖에서 바뀌면 시스템으로 본다
 */
public final class ActorContext {

    public static final String SYSTEM = "시스템";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ActorContext() {
    }

    public static void set(String actor) {
        CURRENT.set(actor);
    }

    public static String current() {
        String actor = CURRENT.get();
        return actor == null ? SYSTEM : actor;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
