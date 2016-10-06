package edu.unc.sol.service;

import edu.unc.sol.app.Optimization;
import edu.unc.sol.app.PathUpdateListener;
import edu.unc.sol.app.TrafficClass;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;

import java.util.List;

@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
public interface SolService {
    void registerApp(ApplicationId id, List<TrafficClass> trafficClasses, Optimization opt,
                     PathUpdateListener listener);
    void unregisterApp(ApplicationId id);
    void updateTrafficClasses(ApplicationId id, List<TrafficClass> trafficClasses);
    void addListener(ApplicationId id, PathUpdateListener listener);
    void removeListener(ApplicationId id, PathUpdateListener listener);
}
