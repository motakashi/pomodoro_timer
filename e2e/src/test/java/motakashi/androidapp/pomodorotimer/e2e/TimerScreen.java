package motakashi.androidapp.pomodorotimer.e2e;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;

final class TimerScreen {
    private final WebDriverWait wait;
    private final Function<TimerElement, By> locators;

    TimerScreen(WebDriver driver, Function<TimerElement, By> locators) {
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.locators = locators;
    }

    void click(TimerElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(locators.apply(element))).click();
    }

    String text(TimerElement element) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locators.apply(element))).getText();
    }

    void waitForText(TimerElement element, String expected) {
        wait.until(driver -> expected.equals(driver.findElement(locators.apply(element)).getText()));
    }

    void waitForDifferentText(TimerElement element, String previous) {
        wait.until(driver -> !driver.findElement(locators.apply(element)).getText().equals(previous));
    }

    void waitUntilVisible(TimerElement element) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locators.apply(element)));
    }
}
