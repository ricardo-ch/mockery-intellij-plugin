package ch.ricardo.plugins.intellij.mockery;

import com.intellij.openapi.components.ApplicationComponent;

public class MockeryGenPlugin implements ApplicationComponent {
    @Override
    public String getComponentName() {
        return "MockeryGen";
    }
}
