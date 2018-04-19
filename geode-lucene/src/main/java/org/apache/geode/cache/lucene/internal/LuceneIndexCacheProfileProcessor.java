package org.apache.geode.cache.lucene.internal;

//
//
//
//
//
// public class LuceneIndexCacheProfileProcessor {
// public static Logger logger = LogService.getLogger();
// protected final DistributionAdvisee userRegion;
// private final LuceneIndexCreationProfile localProfile;
// private ReplyProcessor21 processor;
//
// public LuceneIndexCacheProfileProcessor(
// DistributionAdvisee userRegion, LuceneIndexCreationProfile localProfile) {
// this.userRegion = userRegion;
// this.localProfile = localProfile;
// }
// protected Set getRecipients() {
// DistributionAdvisee parent = this.userRegion.getParentAdvisee();
// Set recps = null;
// if (parent == null) { // root region, all recipients
// InternalDistributedSystem system = this.userRegion.getSystem();
// recps = system.getDistributionManager().getOtherDistributionManagerIds();
// } else {
// // get recipients that have the parent region defined as distributed.
// recps = getAdvice();
// }
// return recps;
// }
// private Set getAdvice() {
// if (this.userRegion instanceof BucketRegion) {
// return ((Bucket) this.userRegion).getBucketAdvisor().adviseProfileExchange();
// } else {
// DistributionAdvisee rgn = this.userRegion.getParentAdvisee();
// DistributionAdvisor advisor = rgn.getDistributionAdvisor();
// return advisor.adviseGeneric();
// }
// }
//
// public void getRemoteCacheProfiles(){
// DistributionManager mgr = this.userRegion.getDistributionManager();
// Set remoteRecipients = getRecipients();
//
// if (remoteRecipients.isEmpty()) {
// return;
// }
//
// ReplyProcessor21 processor = null;
// InternalDistributedSystem system = this.userRegion.getSystem();
// String regionPath = userRegion.getFullPath();
// processor = new LuceneIndexCacheProfileProcessor.LuceneIndexCacheProfileReplyProcessor(system,
// remoteRecipients, this.localProfile, regionPath);
// LuceneIndexCacheProfileProcessor.LuceneIndexCacheProfileMessage
// message = getLuceneIndexCacheProfileMessage(remoteRecipients,processor);
// mgr.putOutgoing(message);
// this.processor = processor;
// waitForProfileResponse();
//
// }
//
// public void waitForProfileResponse() {
// if (processor == null) {
// return;
// }
// DistributionManager mgr = this.userRegion.getDistributionManager();
// try {
// // bug 36983 - you can't loop on a reply processor
// mgr.getCancelCriterion().checkCancelInProgress(null);
// try {
// processor.waitForRepliesUninterruptibly();
// } catch (ReplyException e) {
// e.handleCause();
// }
// } finally {
// processor.cleanup();
// }
// }
//
//
//
// protected LuceneIndexCacheProfileMessage getLuceneIndexCacheProfileMessage(Set recps,
// ReplyProcessor21 proc) {
// LuceneIndexCacheProfileMessage msg = new LuceneIndexCacheProfileMessage();
// msg.regionPath = this.userRegion.getFullPath();
// msg.profile = this.userRegion.getProfile();
// msg.processorId = proc.getProcessorId();
// msg.setRecipients(recps);
// return msg;
// }
//
// public static class LuceneIndexCacheProfileMessage extends HighPriorityDistributionMessage
// implements MessageWithReply {
//
// protected String regionPath;
// protected int processorId = 0;
// private transient ReplyException replyException;
// protected DistributionAdvisor.Profile profile;
//
// @Override
// protected void process(ClusterDistributionManager dm) {
// Throwable thr = null;
// boolean sendReply = this.processorId != 0;
// List<DistributionAdvisor.Profile> replyProfiles = new ArrayList<>();
// try {
// InternalCache cache = dm.getExistingCache();
// }catch (CancelException e) {
// if (logger.isDebugEnabled()) {
// logger.debug("<cache closed> ///{}", this);
// }
// } catch (VirtualMachineError err) {
// SystemFailure.initiateFailure(err);
// // If this ever returns, rethrow the error. We're poisoned
// // now, so don't let this thread continue.
// throw err;
// } catch (Throwable t) {
// // Whenever you catch Error or Throwable, you must also
// // catch VirtualMachineError (see above). However, there is
// // _still_ a possibility that you are dealing with a cascading
// // error condition, so you also need to check to see if the JVM
// // is still usable:
// SystemFailure.checkFailure();
// thr = t;
// } finally {
// if (sendReply) {
// ReplyException rex = null;
// if (thr != null) {
// rex = new ReplyException(thr);
// }
// DistributionAdvisor.Profile[]
// profiles =
// new DistributionAdvisor.Profile[replyProfiles.size()];
// replyProfiles.toArray(profiles);
// LuceneIndexCacheProfileReplyMessage
// .send(getSender(), this.processorId, rex, dm, profiles);
// }
// }
// }
//
//
//
// @Override
// public int getDSFID() {
// return 0;
// }
// }
//
//
// public static class LuceneIndexCacheProfileReplyMessage extends ReplyMessage {
// DistributionAdvisor.Profile[] profiles;
// public static void send(InternalDistributedMember recipient, int processorId,
// ReplyException exception, ClusterDistributionManager dm, DistributionAdvisor.Profile[] profiles)
// {
// Assert.assertTrue(recipient != null, "Sending a ProfilesReplyMessage to ALL");
// LuceneIndexCacheProfileReplyMessage replyMessage = new LuceneIndexCacheProfileReplyMessage();
// replyMessage.processorId = processorId;
// replyMessage.profiles = profiles;
// if (exception != null) {
// replyMessage.setException(exception);
// if (logger.isDebugEnabled()) {
// logger.debug("Replying with exception: {}" + replyMessage, exception);
// }
// }
// replyMessage.setRecipient(recipient);
// dm.putOutgoing(replyMessage);
// }
//
// @Override
// public int getDSFID() {
// return super.getDSFID();
// }
// }
//
// class LuceneIndexCacheProfileReplyProcessor extends ReplyProcessor21 {
//
// LuceneIndexCreationProfile localProfile;
// String regionPath;
//
// public LuceneIndexCacheProfileReplyProcessor(InternalDistributedSystem system,
// Set members,
// LuceneIndexCreationProfile localProfile,
// String regionPath) {
// super(system, members);
// this.localProfile = localProfile;
// this.regionPath = regionPath;
// }
//
// @Override
// public void process(DistributionMessage msg) {
// try {
// if (msg instanceof LuceneIndexCacheProfileReplyMessage) {
// String localID = localProfile.getId();
// LuceneIndexCacheProfileReplyMessage
// reply = (LuceneIndexCacheProfileReplyMessage) msg;
// if (reply.profiles != null) {
// for (int i = 0; i < reply.profiles.length; i++) {
// //Write the code to handle the profile checks.
// CacheDistributionAdvisor.CacheProfile cacheProfile =
// ((CacheDistributionAdvisor.CacheProfile)reply.profiles[i]);
// for(CacheServiceProfile cacheServiceProfile : cacheProfile.cacheServiceProfiles){
// if(cacheServiceProfile instanceof LuceneIndexCreationProfile){
// if(cacheServiceProfile.getId().equals(localID)){
// cacheServiceProfile.checkCompatibility(regionPath, localProfile);
//
// }
// }
// }
// }
// }
// }
// } finally {
// super.process(msg);
// }
// }
// }
//
//
//
//
// }
