import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;

public class PluginInJava implements Plugin<Project> {
  @Override public void apply(Project project) {
    ExtensionAware deps = (ExtensionAware) project.getDependencies();

    System.out.println("DEPENDENCIES: " + deps);
    System.out.println(" -> EXTENSIONS: " + deps.getExtensions());
    System.out.println(" -> EXT: " + deps.getExtensions().findByName("ext"));

    ExtraPropertiesExtension e = (ExtraPropertiesExtension) deps.getExtensions().getByName("ext");
    System.out.println(" -> " + e.getProperties());
    if (true) {
      throw new RuntimeException("EXT: " + e.getProperties());
    }

    System.out.println("ANDROID: " + project.getExtensions().findByName("android"));
  }
}
