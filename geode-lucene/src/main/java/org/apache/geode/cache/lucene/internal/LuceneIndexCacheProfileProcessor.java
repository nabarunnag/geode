package org.apache.geode.cache.lucene.internal;

import java.util.Set;

import org.apache.logging.log4j.Logger;

import org.apache.geode.distributed.internal.ClusterDistributionManager;
import org.apache.geode.distributed.internal.DistributionAdvisee;
import org.apache.geode.distributed.internal.DistributionAdvisor;
import org.apache.geode.distributed.internal.DistributionManager;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.distributed.internal.HighPriorityDistributionMessage;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.distributed.internal.MessageWithReply;
import org.apache.geode.distributed.internal.ReplyProcessor21;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.cache.BucketRegion;
import org.apache.geode.internal.cache.UpdateAttributesProcessor;
import org.apache.geode.internal.cache.partitioned.Bucket;
import org.apache.geode.internal.logging.LogService;


public class LuceneIndexCacheProfileProcessor {
  public static Logger logger = LogService.getLogger();
  protected final DistributionAdvisee userRegion;
  private ReplyProcessor21 processor;

  public LuceneIndexCacheProfileProcessor(
      DistributionAdvisee userRegion) {
    this.userRegion = userRegion;
  }
  protected Set getRecipients() {
    DistributionAdvisee parent = this.userRegion.getParentAdvisee();
    Set recps = null;
    if (parent == null) { // root region, all recipients
      InternalDistributedSystem system = this.userRegion.getSystem();
      recps = system.getDistributionManager().getOtherDistributionManagerIds();
    } else {
      // get recipients that have the parent region defined as distributed.
      recps = getAdvice();
    }
    return recps;
  }
  private Set getAdvice() {
    if (this.userRegion instanceof BucketRegion) {
      return ((Bucket) this.userRegion).getBucketAdvisor().adviseProfileExchange();
    } else {
      DistributionAdvisee rgn = this.userRegion.getParentAdvisee();
      DistributionAdvisor advisor = rgn.getDistributionAdvisor();
      return advisor.adviseGeneric();
    }
  }



    protected LuceneIndexCacheProfileMessage getLuceneIndexCacheProfileMessage(Set recps, ReplyProcessor21 proc,
    boolean useMcast) {
      LuceneIndexCacheProfileMessage msg = new LuceneIndexCacheProfileMessage();
      msg.regionPath = this.newRegion.getFullPath();
      msg.profile = (CacheProfile) this.newRegion.getProfile();
      msg.processorId = proc.getProcessorId();
      msg.concurrencyChecksEnabled = this.newRegion.getAttributes().getConcurrencyChecksEnabled();
      msg.setMulticast(useMcast);
      msg.setRecipients(recps);
      return msg;
    }

  public static class LuceneIndexCacheProfileMessage extends HighPriorityDistributionMessage
      implements MessageWithReply {

    protected int processorId = 0;

    @Override
    protected void process(ClusterDistributionManager dm) {

    }

    @Override
    public int getDSFID() {
      return 0;
    }
  }



  public void getRemoteCacheProfiles(){
    DistributionManager mgr = this.userRegion.getDistributionManager();
    Set remoteRecipients = getRecipients();

    if (remoteRecipients.isEmpty()) {
      return;
    }

    ReplyProcessor21 processor = null;
    InternalDistributedSystem system = this.userRegion.getSystem();
    processor = new LuceneIndexCacheProfileProcessor.LuceneIndexCacheProfileReplyProcessor(system, remoteRecipients);
    LuceneIndexCacheProfileProcessor.LuceneIndexCacheProfileMessage
        message = getUpdateAttributesMessage(processor, remoteRecipients);
    mgr.putOutgoing(message);
    this.processor = processor;

  }

  class LuceneIndexCacheProfileReplyProcessor extends ReplyProcessor21 {

    public LuceneIndexCacheProfileReplyProcessor(InternalDistributedSystem system,
                                                 Set members) {
      super(system, members);
    }

    @Override
    public void process(DistributionMessage msg) {
        if (msg instanceof UpdateAttributesProcessor.ProfilesReplyMessage) {
          LuceneIndexCacheProfileProcessor.ProfilesReplyMessage
              reply = (LuceneIndexCacheProfileProcessor.ProfilesReplyMessage) msg;
          if (reply.profiles != null) {
            for (int i = 0; i < reply.profiles.length; i++) {
              // @todo Add putProfiles to DistributionAdvisor to do this
              // with one call atomically?
              LuceneIndexCacheProfileProcessor.this.advisee.getDistributionAdvisor()
                  .putProfile(reply.profiles[i]);
            }
          }
        } else if (msg instanceof LuceneIndexCacheProfileProcessor.ProfileReplyMessage) {
          LuceneIndexCacheProfileProcessor.ProfileReplyMessage
              reply = (UpdateAttributesProcessor.ProfileReplyMessage) msg;
          if (reply.profile != null) {
            LuceneIndexCacheProfileProcessor.this.advisee.getDistributionAdvisor()
                .putProfile(reply.profile);
          }
        }
    }
  }




}
