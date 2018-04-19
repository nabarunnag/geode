package org.apache.geode.cache.lucene.internal;

import java.util.Map;
import java.util.Set;

import org.apache.geode.distributed.internal.ReplyProcessor21;
import org.apache.geode.internal.cache.CacheDistributionAdvisee;
import org.apache.geode.internal.cache.CacheDistributionAdvisor;
import org.apache.geode.internal.cache.CacheServiceProfile;
import org.apache.geode.internal.cache.CreateRegionProcessor;
import org.apache.geode.internal.cache.LocalRegion;

public class CreateRegionProcessorForLucene extends CreateRegionProcessor {

  /**
   * Creates a new instance of CreateRegionProcessor
   */
  public CreateRegionProcessorForLucene(CacheDistributionAdvisee newRegion) {
    super(newRegion);
  }

  @Override
  protected CreateRegionProcessor.CreateRegionMessage getCreateRegionMessage(Set recps,
                                                                             ReplyProcessor21 proc,
                                                                             boolean useMcast) {
    System.out.println("NABA in the child get Create region message");
    CreateRegionMessageForLucene msg = new CreateRegionMessageForLucene(this.newRegion.getFullPath(), (CacheDistributionAdvisor.CacheProfile) this.newRegion.getProfile(), proc.getProcessorId());
    msg.concurrencyChecksEnabled = this.newRegion.getAttributes().getConcurrencyChecksEnabled();
    msg.setMulticast(useMcast);
    msg.setRecipients(recps);
    return msg;

  }

  public static class CreateRegionMessageForLucene extends CreateRegionProcessor.CreateRegionMessage {


    public CreateRegionMessageForLucene(String fullPath,
                                        CacheDistributionAdvisor.CacheProfile profile,
                                        int processorId) {

      this.regionPath = fullPath;
      this.profile = profile;
      this.processorId = processorId;

    }

    protected String checkCompatibility(CacheDistributionAdvisee rgn,
        CacheDistributionAdvisor.CacheProfile profile) {
      System.out.println("NABA in child checkCompatibiltiy");
      String cspResult = null;
      Map<String, CacheServiceProfile> myProfiles = ((LocalRegion) rgn).getCacheServiceProfiles();
      for (CacheServiceProfile remoteProfile : profile.cacheServiceProfiles) {
        CacheServiceProfile localProfile = myProfiles.get(remoteProfile.getId());
        if (localProfile != null) {
          cspResult = remoteProfile.checkCompatibility(rgn.getFullPath(), localProfile);
        }
        if (cspResult != null) {
          break;
        }
      }
      return cspResult;
    }
  }

}
