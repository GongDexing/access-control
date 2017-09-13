package club.ibook.security.init;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import club.ibook.security.annotation.UserRole;

@Component
public class RoleHelper {

    private List<String> roleRequests = new ArrayList<String>();

    @PostConstruct
    public void init() throws IOException, SecurityException, ClassNotFoundException {
        ResourcePatternResolver rpr = new PathMatchingResourcePatternResolver();
        Resource[] resources = rpr.getResources("**/*.class");
        for (Resource resource : resources) {
            String path = resource.getURL().getPath().split("classes\\/")[1];
            String className = path.replaceAll("\\/", ".").replace(".class", "");
            Class<?> clazz = Class.forName(className);
            String controllerRequestMappingStr = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                controllerRequestMappingStr = clazz.getAnnotation(RequestMapping.class).path()[0];
            }
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)
                        && method.isAnnotationPresent(UserRole.class)) {
                    String[] roles = method.getAnnotation(UserRole.class).name();
                    if (roles.length == 0) {
                        continue;
                    }
                    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                    for (String role : roles) {
                        addRequestToRole(controllerRequestMappingStr, requestMapping, role);
                    }
                }
            }
        }
    }

    private void addRequestToRole(String controllerRequestMappingStr,
            RequestMapping methodRequestMapping, String role) {
        String mapping = controllerRequestMappingStr + methodRequestMapping.path()[0];
        RequestMethod[] requestMethods = methodRequestMapping.method();
        if (requestMethods.length == 0) {
            roleRequests.add(role + " get " + mapping);
            roleRequests.add(role + " post " + mapping);
        } else {
            for (RequestMethod requestMethod : requestMethods) {
                switch (requestMethod) {
                    case GET:
                        roleRequests.add(role + " get " + mapping);
                        break;
                    case POST:
                        roleRequests.add(role + " post " + mapping);
                        break;
                    case PUT:
                        roleRequests.add(role + " put " + mapping);
                        break;
                    case DELETE:
                        roleRequests.add(role + " delete " + mapping);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public boolean isHasRoleAccess(String role, String method, String mapping) {
        return roleRequests.contains(new StringBuilder(role).append(" ").append(method).append(" ")
                .append(mapping).toString());
    }
}
