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

import club.ibook.security.annotation.AllowAnonymous;
import club.ibook.security.annotation.UserRole;

@Component
public class RoleHelper {

    private List<String> roleRequests = new ArrayList<String>();

    private List<String> allowRequest = new ArrayList<String>();

    @PostConstruct
    public void init() throws IOException, SecurityException, ClassNotFoundException {
        ResourcePatternResolver rpr = new PathMatchingResourcePatternResolver();
        // Resource[] resources =
        // rpr.getResources("classpath*:club/ibook/security/controller/*.class");
        Resource[] resources = rpr.getResources("**/*.class");
        for (Resource resource : resources) {
            System.out.println(resource.getURL().getPath());
            String path = resource.getURL().getPath().split("classes!?\\/")[1];
            String className = path.replaceAll("\\/", ".").replace(".class", "");
            Class<?> clazz = Class.forName(className);
            String controllerPath = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                controllerPath = clazz.getAnnotation(RequestMapping.class).path()[0];
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                String methodPath = controllerPath + methodRequestMapping.path()[0];
                if (method.isAnnotationPresent(AllowAnonymous.class)) {
                    allowRequest.add(methodPath);
                } else if (method.isAnnotationPresent(UserRole.class)) {
                    RequestMethod[] requestMethods = methodRequestMapping.method();
                    String[] roles = method.getAnnotation(UserRole.class).name();
                    if (roles.length == 0) {
                        continue;
                    }
                    for (String role : roles) {
                        addRequestToRole(requestMethods, methodPath, role);
                    }
                }
            }
        }
    }

    private void addRequestToRole(RequestMethod[] requestMethods, String path, String role) {
        if (requestMethods.length == 0) {
            roleRequests.add(role + " get " + path);
            roleRequests.add(role + " post " + path);
        } else {
            for (RequestMethod requestMethod : requestMethods) {
                switch (requestMethod) {
                    case GET:
                        roleRequests.add(role + " get " + path);
                        break;
                    case POST:
                        roleRequests.add(role + " post " + path);
                        break;
                    case PUT:
                        roleRequests.add(role + " put " + path);
                        break;
                    case DELETE:
                        roleRequests.add(role + " delete " + path);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 
     * @param role: 权限角色
     * @param method: HTTP 方法，比如get、post
     * @param mapping: path路径，比如 /staff/order/pending
     * @return 该角色是否有访问mapping的权限，返回true表是有，false表示没有
     */
    public boolean isHasRoleAccess(String role, String method, String mapping) {
        if (allowRequest.contains(mapping)) {
            return true;
        }
        if (role == null) {
            return false;
        }
        String str = new StringBuilder(role).append(" ").append(method).append(" ").append(mapping)
                .toString();
        return roleRequests.contains(str);
    }
}
