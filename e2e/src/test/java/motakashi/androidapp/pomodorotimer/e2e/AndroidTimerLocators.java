package motakashi.androidapp.pomodorotimer.e2e;

import org.openqa.selenium.By;

final class AndroidTimerLocators {
    private static final String RESOURCE_PREFIX = "motakashi.androidapp.pomodorotimer:id/";

    private AndroidTimerLocators() {
    }

    static By locator(TimerElement element) {
        String resourceId = switch (element) {
            case SET_COUNT -> "set_count";
            case SET_DECREASE -> "set_decrease";
            case SET_INCREASE -> "set_increase";
            case BREAK_MINUTES -> "break_minutes";
            case BREAK_DECREASE -> "break_decrease";
            case BREAK_INCREASE -> "break_increase";
            case RESET -> "reset_button";
            case START -> "start_button";
            case SET_PROGRESS -> "set_progress";
            case PHASE -> "phase_label";
            case TIME_REMAINING -> "time_remaining";
            case PAUSE -> "pause_button";
            case RESUME -> "resume_button";
            case END -> "end_button";
        };
        return By.id(RESOURCE_PREFIX + resourceId);
    }
}
