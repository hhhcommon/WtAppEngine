/**== 一、字典类=============================================*/
/**001 字典组[PLAT_DICTM]*/
DROP TABLE IF EXISTS plat_DictM;
CREATE TABLE plat_DictM (
  id         varchar(32)      NOT NULL             COMMENT '字典组表ID(UUID)',
  ownerId    varchar(32)      NOT NULL             COMMENT '所有者Id',
  ownerType  int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '所有者类型(0-系统,1-用户,2-session)',
  dmName     varchar(200)     NOT NULL             COMMENT '字典组名称',
  nPy        varchar(800)                          COMMENT '名称拼音',
  sort       int(5) unsigned  NOT NULL  DEFAULT 0  COMMENT '字典组排序,从大到小排序，越大越靠前',
  isValidate int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '是否生效(1-生效,2-无效)',
  mType      int(1) unsigned  NOT NULL  DEFAULT 3  COMMENT '字典类型(1-系统保留,2-系统,3-自定义)',
  mRef       varchar(4000)                         COMMENT '创建时间',
  descn      varchar(500)                          COMMENT '说明',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  lmTime     timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='001字典组';

/**002 字典项[PLAT_DICTD]*/
DROP TABLE IF EXISTS plat_DictD;
CREATE TABLE plat_DictD (
  id         varchar(32)      NOT NULL             COMMENT '字典项表ID(UUID)',
  mId        varchar(32)      NOT NULL             COMMENT '字典组外键(UUID)',
  pId        varchar(32)      NOT NULL             COMMENT '父结点ID(UUID)',
  sort       int(5) unsigned  NOT NULL  DEFAULT 0  COMMENT '字典项排序,只在本级排序有意义,从大到小排序，越大越靠前',
  isValidate int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '是否生效(1-生效,2-无效)',
  ddName     varchar(200)     NOT NULL             COMMENT '字典项名称',
  nPy        varchar(800)                          COMMENT '名称拼音',
  aliasName  varchar(200)                          COMMENT '字典项别名',
  anPy       varchar(800)                          COMMENT '别名拼音',
  bCode      varchar(50)      NOT NULL             COMMENT '业务编码',
  dType      int(1) unsigned  NOT NULL  DEFAULT 3  COMMENT '字典类型(1-系统保留,2-系统,3-自定义,4引用-其他字典项ID；)',
  dRef       varchar(4000)                         COMMENT '创建时间',
  descn      varchar(500)                          COMMENT '说明',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  lmTime     timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='002字典项';

/**== 二、用户用户组类=============================================*/
/**003 PLAT_USER(用户)*/
DROP TABLE IF EXISTS plat_User;
CREATE TABLE plat_User (
  id             varchar(32)      NOT NULL                COMMENT 'uuid(用户id)',
  userName       varchar(100)               DEFAULT NULL  COMMENT '用户名称——实名',
  loginName      varchar(15)      NOT NULL                COMMENT '登录账号',
  password       varchar(30)                DEFAULT NULL  COMMENT '密码',
  mailAddress    varchar(100)               DEFAULT NULL  COMMENT '邮箱(非空为一索引)',
  mainPhoneNum   varchar(100)               DEFAULT NULL  COMMENT '用户主手机号码',
  innerPhoneNum  varchar(100)               DEFAULT NULL  COMMENT '内部通话号码，VOIP',
  userType       int(1) unsigned  NOT NULL                COMMENT '用户分类：1自然人用户，2机构用户',
  userState      int(1)           NOT NULL  DEFAULT '0'   COMMENT '用户状态，0-2,0代表未激活的用户，1代表已激用户，2代表失效用户,3根据邮箱找密码的用户',
  protraitBig    varchar(300)                             COMMENT '用户头像大',
  protraitMini   varchar(300)                             COMMENT '用户头像小',
  descn          varchar(2000)              DEFAULT NULL  COMMENT '备注',
  cTime          timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  lmTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id),
  UNIQUE KEY loginName(loginName) USING BTREE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='003用户表';

/**004 手机用户使用[WT_MOBILEUSED]*/
/**
 * 记录手机最近一次用某账号登录的情况。
 * 若一个月没有登录，是否就删除掉呢，或者是1年??
 */
DROP TABLE IF EXISTS wt_MobileUsed;
CREATE TABLE wt_MobileUsed (
  id       varchar(32)      NOT NULL             COMMENT 'uuid',
  imei     varchar(100)     NOT NULL             COMMENT '手机串号，手机身份码',
  pcdType  int(2) unsigned  NOT NULL  DEFAULT 1  COMMENT '设备分类：1=手机；2=设备；3=PC，默认1',
  userId   varchar(32)      NOT NULL             COMMENT '用户Id',
  status   varchar(1)       NOT NULL             COMMENT '状态：1-登录；2-注销；',
  lmTime   timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id),
  UNIQUE KEY imei(imei) USING BTREE

)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='004手机用户使用';

/**005 PLAT_GROUP(用户组)*/
DROP TABLE IF EXISTS plat_Group;
CREATE TABLE plat_Group (
  id             varchar(32)      NOT NULL                COMMENT 'uuid(用户组id)',
  groupNum       varchar(32)                              COMMENT '组号，用于公开的号码',
  groupName      varchar(100)     NOT NULL                COMMENT '组名称',
  groupSignature varchar(100)     NOT NULL                COMMENT '组签名，只有管理者可以修改，组内成员都可以看到',
  groupPwd       varchar(100)     NOT NULL                COMMENT '组密码',
  groupImg       varchar(200)                             COMMENT '用户组头像，是指向头像的URL',
  groupType      int(2) unsigned  NOT NULL  DEFAULT 0     COMMENT '用户组类型,0一般组(由用户根据好友创建);1号码组;2密码组',
  pId            varchar(32)      NOT NULL  DEFAULT 0     COMMENT '上级用户组名称，默认0，为根',
  sort           int(5) unsigned  NOT NULL  DEFAULT 0     COMMENT '排序,只在本级排序有意义,从大到小排序，越大越靠前',
  createUserId   varchar(32)      NOT NULL                COMMENT '用户组创建者',
  adminUserIds   varchar(32)                              COMMENT '用户组管理者，非一人',
  descn          varchar(2000)              DEFAULT NULL  COMMENT '备注',
  cTime          timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  lmTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改：每次更新的时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='005用户组表';
ALTER TABLE `plat_group` ADD UNIQUE INDEX `idxGroupNum` (`groupNum`) USING HASH;
/** 目前和树形组相关的字段pId, sort没有用 */

/**006 PLAT_GROUPUSER(用户组成员)*/
DROP TABLE IF EXISTS plat_GroupUser;
CREATE TABLE plat_GroupUser (
  id          varchar(32)    NOT NULL  COMMENT 'uuid(主键)',
  groupId     varchar(32)    NOT NULL  COMMENT 'uuid(用户组Id)',
  userId      varchar(32)    NOT NULL  COMMENT 'uuid(用户Id)',
  inviter     varchar(32)    NOT NULL  COMMENT 'uuid(邀请者Id)',
  groupAlias  varchar(100)   NOT NULL  COMMENT '组别名，用户对这个组所定的另一个名称，默认时为组的名称',
  groupDescn  varchar(2000)            COMMENT '组描述，用户对这个组所定的另一个描述，默认时为组的备注',
  cTime       timestamp      NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='006用户组成员表';

/**007 WT_GROUPINVITE(组邀请/申请表)*/
DROP TABLE IF EXISTS wt_GroupInvite;
CREATE TABLE wt_GroupInvite (
  id               varchar(32)   NOT NULL                COMMENT 'uuid(主键)',
  aUserId          varchar(32)   NOT NULL                COMMENT '邀请用户Id，此用户必须在GroupId所在的组',
  bUserId          varchar(32)   NOT NULL                COMMENT '被请用户Id，此用户必须不在GroupId所在的组',
  groupId          varchar(32)   NOT NULL                COMMENT '邀请的组Id',
  groupManageFlag  int(2)        NOT NULL  DEFAULT 0     COMMENT '组管理员处理类型，只有审核组的邀请需要得到管理员的认可，0未处理,1通过,2拒绝',
  inviteVector     int(2)        NOT NULL  DEFAULT 0     COMMENT '邀请方向(vector)，正数，邀请次数，邀请一次，则增加1；负数，申请次数',
  inviteMessage    varchar(600)                          COMMENT '邀请说明',
  firstInviteTime  timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:首次邀请时间',
  inviteTime       timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '再次邀请时间',
  acceptFlag       int(1)        NOT NULL  DEFAULT 0     COMMENT '邀请状态：0未处理;1邀请成功;2拒绝邀请，3别人成功，4别人拒绝',
  acceptTime       timestamp               DEFAULT CURRENT_TIMESTAMP  COMMENT '接受/拒绝邀请的时间',
  refuseMessage    varchar(32)                           COMMENT '拒绝邀请理由',
  flag             int(1)       NOT NULL   DEFAULT 1     COMMENT '状态，1=正在用的组；2=组已被删除，这样的记录groupId在Group组中不必有关联主键',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='007组邀请/申请列表';
/**当用户组解散后，相关的邀请记录仍然被留下*/

/**008 WT_FRIENDINVITE(好友邀请表)*/
DROP TABLE IF EXISTS wt_FriendInvite;
CREATE TABLE wt_FriendInvite (
  id               varchar(32)   NOT NULL                COMMENT 'uuid(主键)',
  aUserId          varchar(32)   NOT NULL                COMMENT '第一用户Id',
  bUserId          varchar(32)   NOT NULL                COMMENT '第二用户Id',
  inviteVector     int(2)        NOT NULL  DEFAULT 0     COMMENT '邀请方向(vector)，总是第一用户邀请第二用户，且是正整数，邀请一次，则增加1，直到邀请成功',
  inviteMessage    varchar(600)                          COMMENT '当前邀请说明文字',
  firstInviteTime  timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:首次邀请时间',
  inviteTime       timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '本次邀请时间',
  acceptFlag       int(1)        NOT NULL  DEFAULT 0     COMMENT '邀请状态：0未处理;1邀请成功;2拒绝邀请;3已被剔除',
  acceptTime       timestamp               DEFAULT CURRENT_TIMESTAMP  COMMENT '接受/拒绝邀请的时间',
  refuseMessage    varchar(32)                           COMMENT '拒绝邀请理由',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='008好友邀请列表';

/**009 WT_FRIEND_REL(好友关系表)*/
DROP TABLE IF EXISTS wt_Friend_Rel;
CREATE TABLE wt_Friend_Rel (
  id               varchar(32)  NOT NULL  COMMENT 'uuid(主键)',
  aUserId          varchar(32)  NOT NULL  COMMENT '第一用户Id',
  bUserId          varchar(32)  NOT NULL  COMMENT '第二用户Id',
  inviteVector     varchar(600)           COMMENT '邀请方向(vector)，是正整数，并且表示邀请成功的次数',
  inviteTime       timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '邀请成功的时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='009好友列表';
/**此表信息可以根据005表生成，既邀请成功的信息倒入此表*/

/**010 WT_PERSONALIAS(人员别名表)*/
DROP TABLE IF EXISTS wt_PersonAlias;
CREATE TABLE wt_PersonAlias (
  id              varchar(32)  NOT NULL  COMMENT 'uuid(主键)',
  typeId          varchar(32)  NOT NULL  COMMENT '组或分类ID，这个需要特别说明，当为"FRIEND"时，是好友的别名，当为12位时是组Id',
  mainUserId      varchar(32)  NOT NULL  COMMENT '主用户Id',
  aliasUserId     varchar(32)  NOT NULL  COMMENT '别名用户Id',
  aliasName       varchar(600)           COMMENT '别名名称',
  aliasDescn      varchar(600)           COMMENT '别名用户描述',
  lastModifyTime  timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '邀请成功的时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='010人员别名列表';


/**011 vWT_FRIEND_REL(好友关系试图)*/
CREATE OR REPLACE ALGORITHM=UNDEFINED SQL SECURITY DEFINER
VIEW vWt_Friend_Rel AS 
  select id, aUserId aUserId, bUserId bUserId, 0+inviteVector inviteVector, inviteTime from wt_Friend_Rel
  union all
  select id, bUserId aUserId, aUserId bUserId, 0-inviteVector inviteVector, inviteTime from wt_Friend_Rel
;

/**== 三、用户意见=============================================*/
/**012 WT_APPOPINION(应用意见表)*/
DROP TABLE IF EXISTS wt_AppOpinion;
CREATE TABLE wt_AppOpinion (
  id       varchar(32)   NOT NULL  COMMENT 'uuid(主键)',
  imei     varchar(32)   NOT NULL  COMMENT '设备IMEI，为移动端设置，若是PC，则必须是网卡的Mac地址',
  userId   varchar(32)             COMMENT '用户Id',
  opinion  varchar(600)  NOT NULL  COMMENT '所提意见，200汉字',
  cTime    timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间，意见成功提交时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='012App用户意见表';

/**013 WT_APPREOPINION(应用反馈表)*/
DROP TABLE IF EXISTS wt_AppReOpinion;
CREATE TABLE wt_AppReOpinion (
  id         varchar(32)   NOT NULL  COMMENT 'uuid(主键)',
  opinionId  varchar(32)   NOT NULL  COMMENT '意见Id，本反馈是针对那一条意见的',
  userId     varchar(32)   NOT NULL  COMMENT '用户Id，注意这里的用户是员工的Id',
  reOpinion  varchar(600)  NOT NULL  COMMENT '反馈内容，200汉字',
  cTime      timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间，反馈成功提交时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='013App用户意见反馈表';

/**== 四、内容类=============================================*/
/**== 四.1、传统电台=============================================*/
/**014 WT_BROADCAST(电台主表)*/
DROP TABLE IF EXISTS wt_Broadcast;
CREATE TABLE wt_Broadcast (
  id            varchar(32)      NOT NULL  COMMENT 'uuid(主键)',
  bcTitle       varchar(100)     NOT NULL  COMMENT '电台名称',
  bcPubType     int(1) unsigned  NOT NULL  COMMENT '电台所属类型：1-组织表,2-文本',
  bcPubId       varchar(32)                COMMENT '电台所属集团Id',
  bcPublisher   varchar(100)               COMMENT '电台所属集团',
  bcImg         varchar(100)               COMMENT '电台图标',
  bcURL         varchar(100)               COMMENT '电台网址',
  descn         varchar(4000)              COMMENT '电台说明',
  cTime         timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='014电台主表';

/**015 WT_BCLIVEFLOW(电台直播流子表)*/
DROP TABLE IF EXISTS wt_BCLiveFlow;
CREATE TABLE wt_BCLiveFlow (
  id         varchar(32)      NOT NULL             COMMENT 'uuid(主键)',
  bcId       varchar(32)      NOT NULL             COMMENT '电台Id,外键',
  bcSrcType  int(1) unsigned  NOT NULL             COMMENT '来源，类型：1-组织表；2-文本',
  bcSrcId    varchar(32)                           COMMENT '来源Id，当bcScrType=1',
  bcSource   varchar(100)     NOT NULL             COMMENT '来源，名称',
  flowURI    varchar(300)     NOT NULL             COMMENT '直播流URL',
  isMain     int(1) unsigned  NOT NULL  DEFAULT 0  COMMENT '是否是主直播流；1是主直播流',
  descn      varchar(4000)                         COMMENT '直播流描述',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='015电台直播流子表';

/**016 WT_BCFREQUNCE(电台频段)*/
DROP TABLE IF EXISTS wt_BCFrequnce;
CREATE TABLE wt_BCFrequnce (
  id        varchar(32)   NOT NULL             COMMENT 'uuid(主键)',
  bcId      varchar(32)   NOT NULL             COMMENT '电台Id,外键',
  areaCode  varchar(100)  NOT NULL             COMMENT '地区编码',
  areaName  varchar(100)  NOT NULL             COMMENT '地区名称',
  frequnce  varchar(300)  NOT NULL             COMMENT '频段',
  isMain    integer(1)    NOT NULL  DEFAULT 0  COMMENT '是否是主频段；1是主频段',
  cTime     timestamp     NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='016电台频段子表';

/**== 四.2、单体资源 =============================================*/
/**017 WT_MEDIAASSET(媒体资源，文件类聚合，原子性的)*/
DROP TABLE IF EXISTS wt_MediaAsset;
CREATE TABLE wt_MediaAsset (
  id              varchar(32)      NOT NULL  COMMENT 'uuid(主键)',
  maTitle         varchar(100)     NOT NULL  COMMENT '媒体资源名称',
  maPubType       int(1) unsigned  NOT NULL  COMMENT '发布类型：1-组织表,2-文本',
  maPubId         varchar(32)      NOT NULL  COMMENT '发布所属组织',
  maPublisher     varchar(100)               COMMENT '发布者',
  maPublisherTime timestamp                  COMMENT '发布时间',
  maImg           varchar(100)               COMMENT '媒体图',
  maURL           varchar(100)               COMMENT '媒体主地址，可以是聚合的源，也可以是Wt平台中的文件URL',
  subjectWord     varchar(400)               COMMENT '主题词',
  keyWord         varchar(400)               COMMENT '关键词',
  langDid         varchar(32)                COMMENT '语言字典项Id',
  language        varchar(32)                COMMENT '语言名称',
  timeLong        long                       COMMENT '时长，毫秒数',
  descn           varchar(4000)              COMMENT '说明',
  cTime           timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='017媒体资源';

/**== 四.3、系列媒体 =============================================*/
/**018 WT_SEQMEDIAASSET(系列媒体资源)*/
DROP TABLE IF EXISTS wt_SeqMediaAsset;
CREATE TABLE wt_SeqMediaAsset (
  id                varchar(32)      NOT NULL  COMMENT 'uuid(主键)',
  smaTitle          varchar(100)     NOT NULL  COMMENT '系列媒体资源名称',
  smaPubType        int(1) unsigned  NOT NULL  COMMENT '发布类型：1-组织表,2-文本',
  smaPubId          varchar(32)      NOT NULL  COMMENT '发布所属组织',
  smaPublisher      varchar(100)               COMMENT '发布者',
  smaPublisherTime  timestamp                  COMMENT '发布时间',
  smaImg            varchar(100)               COMMENT '媒体图',
  smaAllCount       int unsigned     NOT NULL  COMMENT '总卷集号，可以为空，这个和总数不同，也可能一样',
  subjectWord       varchar(400)               COMMENT '主题词',
  keyWord           varchar(400)               COMMENT '关键词',
  langDid           varchar(32)                COMMENT '语言字典项Id',
  language          varchar(32)                COMMENT '语言名称',
  descn             varchar(4000)              COMMENT '说明',
  cTime             timestamp    NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='018系列媒体资源';

/**019 WT_MASOURCE(资产来源以及播放地址)*/
DROP TABLE IF EXISTS wt_MaSource;
CREATE TABLE wt_MaSource (
  id         varchar(32)      NOT NULL             COMMENT 'uuid(主键)',
  maId       varchar(32)      NOT NULL             COMMENT '媒体Id,外键',
  maSrcTppe  int(1) unsigned  NOT NULL             COMMENT '来源，类型：1-组织表；2-文本',
  maSrcId    varchar(32)      NOT NULL             COMMENT '来源，描述',
  maSource   varchar(100)     NOT NULL             COMMENT '来源，名称',
  smType     int(1) unsigned  NOT NULL             COMMENT '来源媒体分类:1-文件;2-直播流',
  playURI    varchar(300)     NOT NULL             COMMENT '直播流URL',
  isMain     integer(1)       NOT NULL  DEFAULT 0  COMMENT '是否主播放地址；1是主播放',
  descn      varchar(4000)                         COMMENT '来源说明',
  cTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='019资产来源以及播放地址';

/**020 WT_SEQMA_REF(系列媒体与单体媒体对应表)*/
DROP TABLE IF EXISTS wt_SeqMA_Ref;
CREATE TABLE wt_SeqMA_Ref (
  id          varchar(32)    NOT NULL  COMMENT 'uuid(主键)',
  sId         varchar(32)    NOT NULL  COMMENT '系列Id,主表Id',
  mId         varchar(32)    NOT NULL  COMMENT '媒体Id',
  columnNum   int  unsigned  NOT NULL  COMMENT '卷集号，也是排序号',
  descn       varchar(4000)            COMMENT '关联说明',
  cTime       timestamp      NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='020系列媒体与单体媒体对应表';

/**== 四.4、外围对象 =============================================*/
/**021 WT_PERSON(干系人，主要是人的自然信息，和User不同)*/
DROP TABLE IF EXISTS wt_Person;
CREATE TABLE wt_Person (
  id            varchar(32)      NOT NULL                COMMENT 'uuid(用户id)',
  pName         varchar(100)     NOT NULL                COMMENT '名称',
  age           varchar(15)                              COMMENT '年龄',
  birthday      varchar(30)                DEFAULT NULL  COMMENT '生日',
  sex           varchar(100)                             COMMENT '性别',
  descn         varchar(4000)              DEFAULT NULL  COMMENT '人员描述',
  phoneNum      varchar(100)               DEFAULT NULL  COMMENT '人员手机',
  email         varchar(100)               DEFAULT NULL  COMMENT 'eMail',
  homepage      varchar(100)                             COMMENT '个人主页',
  protraitBig   varchar(300)                             COMMENT '用户头像大',
  protraitMini  varchar(300)                             COMMENT '用户头像小',
  cTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='021干系人';

/**022 WT_ORGANIZE(组织机构，和Group不同)*/
DROP TABLE IF EXISTS wt_Organize;
CREATE TABLE wt_Organize (
  id            varchar(32)      NOT NULL                COMMENT 'uuid(用户id)',
  oName         varchar(100)     NOT NULL                COMMENT '名称',
  descn         varchar(100)               DEFAULT NULL  COMMENT '说明',
  webPage       varchar(100)               DEFAULT NULL  COMMENT '官网地址',
  orgTypeId     varchar(100)                             COMMENT '组织分类Id，可分为：电台、网站等',
  orgTypeName   varchar(100)                             COMMENT '组织分类名称',
  protraitBig   varchar(300)                             COMMENT '组织logo大图像',
  protraitMini  varchar(300)                             COMMENT '组织logo小图像',
  cTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='022组织机构';

/**== 四.5、各类关系关联 =============================================*/
/**023 WT_PERSON_REF(干系人与资源关系)*/
DROP TABLE IF EXISTS wt_Person_Ref;
CREATE TABLE wt_Person_Ref (
  id         varchar(32)    NOT NULL                COMMENT 'uuid(id)',
  personId   varchar(32)    NOT NULL                COMMENT '用户Id',
  resType    varchar(32)    NOT NULL                COMMENT '资源类型Id：1电台；2单体媒体资源；3系列媒体资源，4栏目',
  resId      varchar(32)    NOT NULL                COMMENT '资源Id',
  refTypeId  varchar(32)    NOT NULL                COMMENT '关联类型，是字典项，是专门的一个字典组',
  cName      varchar(200)   NOT NULL                COMMENT '字典项名称',
  descn      varchar(100)             DEFAULT NULL  COMMENT '描述',
  cTime      timestamp      NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间:创建时的系统时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='023干系人与资源关系';

/**024 WT_RESCATA_REF(资源分类对应关系)*/
DROP TABLE IF EXISTS wt_ResCata_Ref;
CREATE TABLE wt_ResCata_Ref (
  id         varchar(32)    NOT NULL  COMMENT 'uuid(主键)',
  resType    varchar(32)    NOT NULL  COMMENT '资源类型Id：1电台；2单体媒体资源；3系列媒体资源',
  resId      varchar(32)    NOT NULL  COMMENT '资源Id',
  dictMid    varchar(32)    NOT NULL  COMMENT '字典组Id',
  dictMName  varchar(200)   NOT NULL  COMMENT '字典组名称',
  dictDid    varchar(32)    NOT NULL  COMMENT '字典项Id',
  title      varchar(200)   NOT NULL  COMMENT '字典项名称',
  bCode      varchar(200)   NOT NULL  COMMENT '字典项业务编码',
  pathNames  varchar(1000)  NOT NULL  COMMENT '字典项全名称',
  pathIds    varchar(100)   NOT NULL  COMMENT '字典项路径Id',
  cTime      timestamp      NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY(id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='024电台分类表';

/**== 五、号码黑名单 =============================================*/
/**025 WT_BLACK_GNUM(组号黑名单，名单中号码不会出现在组号中)*/
DROP TABLE IF EXISTS wt_Black_GNum;
CREATE TABLE wt_Black_GNum (
  groupNum  int(16) unsigned  NOT NULL  COMMENT '黑名单号码',
  PRIMARY KEY(groupNum)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='025组号黑名单';

/**== 六、栏目及发布管理 =============================================*/
/**026 栏目表[WT_CHANNEL]*/
DROP TABLE IF EXISTS wt_Channel;
CREATE TABLE wt_Channel (
  id          varchar(32)      NOT NULL             COMMENT '表ID(UUID)',
  pcId        varchar(32)      NOT NULL             COMMENT '父结点ID(UUID)，若是根为ROOT',
  ownerId     varchar(32)      NOT NULL  DEFAULT 1  COMMENT '所有者Id，目前完全是系统维护的栏目，为1',
  ownerType   int(1) unsigned  NOT NULL  DEFAULT 0  COMMENT '所有者类型(0-系统,1-主播)，目前为0',
  chanelName  varchar(200)     NOT NULL             COMMENT '栏目名称',
  nPy         varchar(800)                          COMMENT '名称拼音',
  sort        int(5) unsigned  NOT NULL  DEFAULT 0  COMMENT '栏目排序,从大到小排序，越大越靠前，根下同级别',
  isValidate  int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '是否生效(1-生效,2-无效)',
  contentType varchar(40)      NOT NULL  DEFAULT 0  COMMENT '允许资源的类型，可以是多个，0所有；1电台；2单体媒体资源；3系列媒体资源；用逗号隔开，比如“1,2”，目前都是0',
  mRef        varchar(4000)                         COMMENT '创建时间',
  channelImg  varchar(200)                          COMMENT '栏目图片Id',
  descn       varchar(500)                          COMMENT '栏目说明',
  cTime       timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  lmTime      timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '最后修改时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='026栏目表';
/**栏目的编辑等干系人信息在，干系人与资源关系表023**/

/**027 栏目内容发布表[WT_CHANNELASSET]*/
DROP TABLE IF EXISTS wt_ChannelAsset;
CREATE TABLE wt_ChannelAsset (
  id            varchar(32)      NOT NULL             COMMENT '表ID(UUID)',
  channelId     varchar(32)      NOT NULL             COMMENT '栏目Id',
  assetType     varchar(32)      NOT NULL             COMMENT '内容类型：1电台；2单体媒体资源；3系列媒体资源',
  assetId       varchar(32)      NOT NULL             COMMENT '内容Id',
  publisherId   varchar(32)      NOT NULL             COMMENT '发布者Id',
  checkerId     varchar(32)                           COMMENT '审核者Id，可以为空，若为1，则审核者为系统',
  flowFlag      int(1) unsigned  NOT NULL  DEFAULT 0  COMMENT '流程状态：0入库；1在审核；2审核通过(既发布状态)；3审核未通过',
  sort          int(5) unsigned  NOT NULL  DEFAULT 0  COMMENT '栏目排序,从大到小排序，越大越靠前，既是置顶功能',
  isValidate    int(1) unsigned  NOT NULL  DEFAULT 1  COMMENT '是否生效(1-生效,2-无效)',
  pubName       varchar(200)                          COMMENT '发布名称，可为空，若为空，则取资源的名称',
  pubImg        varchar(500)                          COMMENT '发布图片，可为空，若为空，则取资源的Img',
  inRuleIds     varchar(100)                          COMMENT '进入该栏目的规则，0为手工/人工创建，其他未系统规则Id',
  checkRuleIds  varchar(100)                          COMMENT '审核规则，0为手工/人工创建，其他为系统规则id',
  cTime         timestamp        NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  pubTime       timestamp                             COMMENT '发布时间，发布时的时间，若多次发布，则是最新的发布时间',
  PRIMARY KEY (id)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='027栏目内容发布';

/**关于规则，在下一期处理，本期都写死在代码中 **/


/*****************************************/
/**
CREATE OR REPLACE ALGORITHM=UNDEFINED SQL SECURITY DEFINER
VIEW vWt_Friend_Rel AS 
  select a.id, a.aUserId aUserId, b.loginName aUserName, b.protraitMini aProtraitUri,
    a.bUserId bUserId, c.loginName bUserName, c.protraitMini bProtraitUri,
    0+a.inviteVector inviteVector, a.inviteTime
  from wt_Friend_Rel a
  left join plat_User b on a.aUserId=b.id
  left join plat_User c on a.bUserId=c.id
  union all
  select d.id, d.bUserId aUserId, e.loginName aUserName, e.protraitMini aProtraitUri,
    d.aUserId bUserId, f.loginName bUserName, f.protraitMini bProtraitUri,
    0-d.inviteVector inviteVector, d.inviteTime
  from wt_Friend_Rel d
  left join plat_User e on d.aUserId=e.id
  left join plat_User f on d.bUserId=f.id
;
**/
/*****************************************/
