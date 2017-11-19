# access-control
一个超级实用的简单权限控制应用，通过注释实现对接口的权限控制，没有**UserRole**注释的接口，是无法访问

### 一个栗子
```java
@RestController
@RequestMapping(path = "/test")
public class TestController {

    @UserRole(name = {"admin"})
    @RequestMapping(path = "/admin", method = RequestMethod.GET)
    public String test() {
        return "test";
    }

    @UserRole(name = {"reader"})
    @RequestMapping(path = "/reader", method = RequestMethod.GET)
    public String good() {
        return "good";
    }

    @RequestMapping(path = "/no", method = RequestMethod.GET)
    public String no() {
        return "no";
    }

    @AllowAnonymous
    @RequestMapping(path = "/all", method = RequestMethod.GET)
    public String all() {
        return "all";
    }
}
```
### 测试结果
```sh
$ curl "http://127.0.0.1:8080/test/admin" --header "role:admin"
test

$ curl "http://127.0.0.1:8080/test/admin" --header "role:reader"
{"timestamp":1505312963388,"status":401,"error":"Unauthorized","message":"not authorized","path":"/test/admin"}

$ curl "http://127.0.0.1:8080/test/admin"
{"timestamp":1505312968966,"status":401,"error":"Unauthorized","message":"not authorized","path":"/test/admin"}

$ curl "http://127.0.0.1:8080/test/no"  --header "role:reader"
{"timestamp":1505313048496,"status":401,"error":"Unauthorized","message":"not authorized","path":"/test/no"}

$ curl "http://127.0.0.1:8080/test/no"  --header "role:admin"
{"timestamp":1505313052588,"status":401,"error":"Unauthorized","message":"not authorized","path":"/test/no"}

$ curl "http://127.0.0.1:8080/test/all"
all
```

### 实现原理

在应用启动时，扫描**classpath**的所有类，找到同时被 **RequestMapping** 和 **UserRole** 注释的方法，将UserRole类的name属性和RequestMapping类的path属性以及Method方法拼接为一个字符串String，放在在List中。当有任何请求访问时，根据Request的Header获得role，ServletPath获得path，Method获得方法，拼接成一个字符串，如果List有这个字符串，则认为该Role有访问path的权限，否则就没有，返回 **401**;
另外，在很多情况下，某些请求不需要做权限控制，比如登陆请求，这时用 **AllowAnonymous** 即可
```java
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
```
