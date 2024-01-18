package id.co.telkom.wfm.plugin;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();
        registrationList.add(context.registerService(GenerateVRF.class.getName(), new GenerateVRF(), null));
        registrationList.add(context.registerService(GenerateVRFNameExisting.class.getName(), new GenerateVRFNameExisting(), null));
        registrationList.add(context.registerService(GenerateVLANReservation.class.getName(), new GenerateVLANReservation(), null));
        registrationList.add(context.registerService(IPVLANConnecitivty.class.getName(), new IPVLANConnecitivty(), null));
        registrationList.add(context.registerService(GeneratePeName.class.getName(), new GeneratePeName(), null));
        registrationList.add(context.registerService(GenerateMeService.class.getName(), new GenerateMeService(), null));
        registrationList.add(context.registerService(GenerateMeAccess.class.getName(), new GenerateMeAccess(), null));
        registrationList.add(context.registerService(GenerateSidConnectivity.class.getName(), new GenerateSidConnectivity(), null));
        registrationList.add(context.registerService(ValidateSto.class.getName(), new ValidateSto(), null));
        registrationList.add(context.registerService(ValidateVrf.class.getName(), new ValidateVrf(), null));
        registrationList.add(context.registerService(GenerateIPReservation.class.getName(), new GenerateIPReservation(), null));
        registrationList.add(context.registerService(GenerateUplinkPort.class.getName(), new GenerateUplinkPort(), null));
        registrationList.add(context.registerService(GenerateDownlinkPort.class.getName(), new GenerateDownlinkPort(), null));
        registrationList.add(context.registerService(GenerateIpV4.class.getName(), new GenerateIpV4(), null));
        registrationList.add(context.registerService(GenerateStpNetLoc.class.getName(), new GenerateStpNetLoc(), null));
        registrationList.add(context.registerService(CheckACS.class.getName(), new CheckACS(), null));
        registrationList.add(context.registerService(Validate.class.getName(), new Validate(), null));
        registrationList.add(context.registerService(ShowCommand.class.getName(), new ShowCommand(), null));
        registrationList.add(context.registerService(GenerateODP.class.getName(), new GenerateODP(), null));
        registrationList.add(context.registerService(GenerateSidNetmonk.class.getName(), new GenerateSidNetmonk(), null));
        registrationList.add(context.registerService(ReservationIDC.class.getName(), new ReservationIDC(), null));
        registrationList.add(context.registerService(ActivationPowerIDC.class.getName(), new ActivationPowerIDC(), null));
        registrationList.add(context.registerService(IDCCompleteConnectivity.class.getName(), new IDCCompleteConnectivity(), null));
        registrationList.add(context.registerService(FeasibilityCNDC.class.getName(), new FeasibilityCNDC(), null));
        registrationList.add(context.registerService(RollbackTaskStatus.class.getName(), new RollbackTaskStatus(), null));
        //Register plugin here
        //registrationList.add(context.registerService(MyPlugin.class.getName(), new MyPlugin(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}
