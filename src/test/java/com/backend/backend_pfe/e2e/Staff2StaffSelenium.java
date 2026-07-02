package com.backend.backend_pfe.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E Selenium suite for the deployed Staff2Staff app.
 *
 * This class intentionally does not end with "Test" so Maven's default
 * unit-test run does not execute browser tests. Run it explicitly with:
 * mvn test -Dtest=*Selenium*
 */
class Staff2StaffSelenium {

    private static final String FRONTEND_URL = System.getProperty("s2s.frontend.url", "http://localhost:5173");
    private static final Duration TIMEOUT = Duration.ofSeconds(Long.getLong("selenium.timeout.seconds", 15L));

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        configureEdgeDriver();

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--window-size=1440,1000", "--remote-allow-origins=*");
        if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new");
        }

        driver = new EdgeDriver(options);
        wait = new WebDriverWait(driver, TIMEOUT);
    }

    private void configureEdgeDriver() {
        String configuredDriver = firstNonBlank(
                System.getProperty("webdriver.edge.driver"),
                System.getProperty("selenium.edge.driver"),
                System.getenv("MSEDGEDRIVER_PATH")
        );

        Assumptions.assumeTrue(configuredDriver != null,
                "EdgeDriver local requis. Lancez avec -Dwebdriver.edge.driver=C:\\Tools\\msedgedriver.exe");

        Path driverPath = Path.of(configuredDriver);
        Assumptions.assumeTrue(Files.isRegularFile(driverPath),
                "EdgeDriver introuvable: " + configuredDriver);
        System.setProperty("webdriver.edge.driver", driverPath.toString());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void resourceManagerCanLoginAndSeeDashboard() {
        login("fz.bennis@soprabanking.com", "Rm@Staff2026!");

        waitForUrlPath("/");
        assertPageContainsAny("Dashboard - Vue Globale", "Dashboard");
    }

    @Test
    void chefProjetCanLoginAndSeeDashboard() {
        login("khalid.bennani@soprabanking.com", "Pm@Staff2026!");

        waitForUrlPath("/pm");
        assertPageContainsAny("Dashboard Chef de Projet", "Suivi de vos projets", "Prévision IA", "Prevision IA");
    }

    @Test
    void collaborateurCanLoginAndSeeDashboard() {
        login("youssef.elamrani@soprabanking.com", "Collab@Staff2026!");

        waitForUrlPath("/collab");
        assertPageContainsAny("Mon Dashboard", "Vue personnelle de vos projets");
    }

    @Test
    void resourceManagerCanNavigateResourcesConflictsAndSimulation() {
        login("fz.bennis@soprabanking.com", "Rm@Staff2026!");

        clickNavigation("Ressources");
        waitForUrlPath("/resources");
        assertPageContainsAny("Gestion des Ressources");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));

        clickNavigation("Conflits");
        waitForUrlPath("/conflicts");
        assertPageContainsAny("Gestion des Conflits");

        List<WebElement> simulationActions = findDisplayedByText(
                "Résoudre par simulation",
                "RÃ©soudre par simulation",
                "Resoudre par simulation"
        );
        Assumptions.assumeFalse(simulationActions.isEmpty(), "Aucun conflit cliquable disponible pour tester la redirection simulation.");

        simulationActions.get(0).click();
        waitForUrlPath("/simulation");
        assertPageContainsAny("Simulation", "Remplacement de Collaborateur", "Affecter un Sous-Chargé", "Affecter un Sous-Charge");
    }

    @Test
    void collaborateurCanOpenScheduleAndTaskTrackingPopupWhenTaskExists() {
        login("youssef.elamrani@soprabanking.com", "Collab@Staff2026!");

        clickNavigation("Mon Planning");
        waitForUrlPath("/collab/schedule");
        assertPageContainsAny("Mon Planning", "Calendrier");

        List<WebElement> tasks = findDisplayedByText("En attente", "En cours", "Terminée", "Terminee", "Bloquée", "Bloquee");
        Assumptions.assumeFalse(tasks.isEmpty(), "Aucune tâche visible dans le calendrier pour tester la popup de suivi.");

        tasks.get(0).click();
        assertPageContainsAny("Suivi de tâche", "Suivi de tache", "Tâche terminée", "Tache terminee");
    }

    @Test
    void userCanClickNotificationWhenOneExists() {
        login("fz.bennis@soprabanking.com", "Rm@Staff2026!");

        String initialUrl = driver.getCurrentUrl();
        openNotificationsDropdown();

        if (pageContains("Aucune notification")) {
            Assumptions.abort("Aucune notification disponible pour tester la redirection.");
        }

        List<WebElement> notificationItems = driver.findElements(By.cssSelector(".s2s-topbar-menu div[style*='cursor: pointer']"))
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(e -> !text(e).contains("Voir toutes les notifications"))
                .toList();
        Assumptions.assumeFalse(notificationItems.isEmpty(), "Aucun élément notification cliquable trouvé dans la Topbar.");

        notificationItems.get(0).click();
        wait.until(d -> !d.getCurrentUrl().equals(initialUrl) || !d.findElements(By.cssSelector(".s2s-topbar-menu")).isEmpty());

        assertTrue(driver.getCurrentUrl().startsWith(FRONTEND_URL), "La navigation doit rester dans l'application Staff2Staff.");
    }

    private void login(String email, String password) {
        driver.get(FRONTEND_URL + "/login");

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']")));
        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));

        clearBrowserState();
        emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']")));
        passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));

        emailInput.clear();
        emailInput.sendKeys(email);
        passwordInput.clear();
        passwordInput.sendKeys(password);
        passwordInput.sendKeys(Keys.ENTER);

        wait.until(d -> !d.getCurrentUrl().contains("/login"));
    }

    private void clearBrowserState() {
        ((JavascriptExecutor) driver).executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
        driver.manage().deleteAllCookies();
        driver.navigate().refresh();
    }

    private void clickNavigation(String label) {
        WebElement link = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText(label)));
        link.click();
    }

    private void openNotificationsDropdown() {
        List<WebElement> headerButtons = wait.until(d -> d.findElements(By.cssSelector("header button"))
                .stream()
                .filter(WebElement::isDisplayed)
                .toList());
        Assumptions.assumeFalse(headerButtons.isEmpty(), "Bouton notifications introuvable dans la Topbar.");
        headerButtons.get(0).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".s2s-topbar-menu")));
    }

    private void waitForUrlPath(String path) {
        wait.until(d -> {
            String current = d.getCurrentUrl();
            return current.equals(FRONTEND_URL + path)
                    || current.startsWith(FRONTEND_URL + path + "?")
                    || current.startsWith(FRONTEND_URL + path + "#");
        });
    }

    private List<WebElement> findDisplayedByText(String... candidates) {
        return List.of(candidates).stream()
                .flatMap(candidate -> driver.findElements(By.xpath("//*[contains(normalize-space(.), " + xpathLiteral(candidate) + ")]")).stream())
                .filter(WebElement::isDisplayed)
                .toList();
    }

    private void assertPageContainsAny(String... candidates) {
        wait.until(d -> List.of(candidates).stream().anyMatch(this::pageContains));
        assertTrue(List.of(candidates).stream().anyMatch(this::pageContains),
                "La page ne contient aucun des textes attendus: " + String.join(", ", candidates));
    }

    private boolean pageContains(String expected) {
        String source = driver.getPageSource().toLowerCase(Locale.ROOT);
        return source.contains(expected.toLowerCase(Locale.ROOT));
    }

    private String text(WebElement element) {
        return element.getText() == null ? "" : element.getText();
    }

    private String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }
}
