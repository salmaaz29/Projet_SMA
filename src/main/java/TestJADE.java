import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;

public class TestJADE {
    public static void main(String[] args) {
        try {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.GUI, "true");
            AgentContainer container = rt.createMainContainer(p);
            System.out.println("✅ JADE fonctionne !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
