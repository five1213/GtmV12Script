//V20181203--修复monit为udp + icmp时出来脚本为tcp的VS的问题
//V20181207修复会话保持为"无"，选择插入客户端地址，同时启用或不启用snat时，VS脚本生成错误问题。
//20181216修复会话保持为"无"，同时启用snat时，VS脚本生成错误问题。
//读取excel的第几个sheet，从0开始计算
int sheet = 0
//读取excel的指定sheet数据时，从第几行开始读取有效数据，从1开始计算
int row = 1
params = readExcel('D:\\F5变更自动化\\test.xlsx', sheet, row)

//生成脚本文件F5.cli
filepath = 'D:\\F5变更自动化\\变更脚本\\'

// prepare to generate CLI script
cli = new StringBuilder()
outPut = new StringBuilder()

outPut << "设备名称" + "," + "命令行" + "," + "\n"
outPut << "DEVICENAME" + "," + "CmdLine" + "," + "\n"

class GtmObject {
    String scriptType
    String deviceName
    String wideip
    String wideipRecordType
    String wideipLoadBalancingMode
    String region
    String score
    String poolLoadBalancingMode
    String poolRecordType
    String dataCenter
    String monitor
    String xianLu
    String destination
    String port
    String resolvingOtherDomain
    String poolCnameOrMxDomin
}


HashMap<String,List<GtmObject>> mapGtm = new HashMap<String,List<GtmObject>>()
HashSet<String> ipAddSet = new HashSet<String>()
HashSet<String> otherIpAddSet = new HashSet<String>()
HashMap<String,GtmObject> resolvingOtherDomainMap = new HashMap<String,GtmObject>()
HashMap<String,String> wideipToPoolsMap = new HashMap<String,String>()
HashMap<String,String> wideipToLbModeMap = new HashMap<String,String>()
HashMap<String,String> poolRegionRecordScoreMap = new HashMap<String,String>()
HashMap<String,String> poolRegionRecordLbModeMap = new HashMap<String,String>()
for (it in params) {
    row = row + 1
    notice = "第"+row+"行："

    scriptType = it.'ScriptType'
    if(scriptType == null || scriptType == ''){
        throw new Exception("${notice}请补全ScriptType信息！")
    }
    deviceName = it.'DeviceName'
    if(deviceName == null || deviceName == ''){
        throw new Exception("${notice}请补全DeviceName信息！")
    }
    wideip = it.'Wideip'
    if(wideip == null || wideip == ''){
        throw new Exception("${notice}请补全Wideip信息！")
    }

    if(!isDomain(wideip)){
        throw new Exception("${notice}请填写正确的Wideip域名信息！")
    }

    String wideipRecordType = it.'WideipRecordType'
    if(wideipRecordType == null || wideipRecordType == ''){
        throw new Exception("${notice}请补全(${wideip})WideipRecordType信息！")
    }

    wideipRecordType = wideipRecordType.toLowerCase()
    if(wideipRecordType != 'a' && wideipRecordType != 'aaaa' && wideipRecordType != 'cname' && wideipRecordType != 'mx'){
        throw new Exception("${notice}请填入(${wideip})正确的WideipRecordType信息！")
    }

    label = deviceName + ":" + wideip + ":" + wideipRecordType
    wideipLoadBalancingMode = it.'WideipLoadBalancingMode'
    if(wideipLoadBalancingMode == null || wideipLoadBalancingMode == ''){
        throw new Exception("${notice}请补全(${label})的WideipLoadBalancingMode信息！")
    }

    wideipLbModeTemp = wideipToLbModeMap.get(label)
    if(wideipLbModeTemp == null || wideipLbModeTemp == ''){
        wideipToLbModeMap.put(label,wideipLoadBalancingMode)
    }else{
        if(wideipLoadBalancingMode != wideipLbModeTemp){
            throw new Exception("${notice}请确保(${label})同一个记录类型的wideip中的WideipLoadBalancingMode相同！")
        }
    }

    if(wideipLoadBalancingMode == 'Global_Availability'){
        wideipLoadBalancingMode = 'global-availability'
    }else if(wideipLoadBalancingMode == 'Ratio'){
        wideipLoadBalancingMode = 'ratio'
    }else if(wideipLoadBalancingMode == 'Round_Robin'){
        wideipLoadBalancingMode = 'round-robin'
    }else if(wideipLoadBalancingMode == 'Topology'){
        wideipLoadBalancingMode = 'topology'
    }else{
        throw new Exception("${notice}请选择(${label})正确的WideipLoadBalancingMode")
    }

    resolvingOtherDomain = it.'ResolvingOtherDomain'
    if(resolvingOtherDomain != null && resolvingOtherDomain != ''){
        if(!isDomain(resolvingOtherDomain)){
            throw new Exception("${notice}请填写(${label})正确的ResolvingOtherDomain域名信息！")
        }
        GtmObject resolvGtm = resolvingOtherDomainMap.get(label)
        if(resolvGtm != null ){
            throw new Exception("${notice}(${label})的ResolvingOtherDomain的类型中存在相同记录类型的wideIp！")
        }
        resolvGtm = new GtmObject()
        resolvGtm.scriptType = scriptType
        resolvGtm.deviceName = deviceName
        resolvGtm.wideip = wideip
        resolvGtm.wideipRecordType = wideipRecordType
        resolvGtm.wideipLoadBalancingMode = wideipLoadBalancingMode
        resolvGtm.resolvingOtherDomain = resolvingOtherDomain
        resolvingOtherDomainMap.put(label,resolvGtm)
        continue
    }

    String poolRecordType = it.'PoolRecordType'
    if(poolRecordType == null || poolRecordType == ''){
        throw new Exception("${notice}请补全(${label})的PoolRecordType信息！")
    }

    poolRecordType = poolRecordType.toLowerCase()

    if(wideipRecordType == 'a'){
        if(poolRecordType != 'a' && poolRecordType != 'cname'){
            throw new Exception("${notice}(${label})的wideip为A记录时，PoolRecordType的类型必须为A或者CNAME！")
        }
    }

    if(wideipRecordType == 'aaaa'){
        if(poolRecordType != 'aaaa' && poolRecordType != 'cname'){
            throw new Exception("${notice}(${label})的wideip为AAAA记录时，PoolRecordType的类型必须为AAAA或者CNAME！")
        }
    }

    if(wideipRecordType == 'cname'){
        if(poolRecordType != 'cname'){
            throw new Exception("${notice}(${label})的wideip为CNAME记录时，PoolRecordType的类型必须为CNAME！")
        }
    }

    if(wideipRecordType == 'mx'){
        if(poolRecordType != 'mx' && poolRecordType != 'cname'){
            throw new Exception("${notice}(${label})的wideip为MX记录时，PoolRecordType的类型必须为MX或者CNAME！")
        }
    }

    poolCnameOrMxDomin = it.'PoolCnameOrMxDomin'
    if(poolRecordType == 'cname' || poolRecordType == 'mx'){
        if(poolCnameOrMxDomin == null || poolCnameOrMxDomin == ''){
            throw new Exception("${notice}请补全(${label})的PoolCnameOrMxDomin信息！")
        }

        if(!isDomain(poolCnameOrMxDomin)){
            throw new Exception("${notice}请填写(${label})的正确的PoolCnameOrMxDomin信息！")
        }
    }

    region = it.'Region'
    if(region == null || region == ''){
        throw new Exception("${notice}请补全(${label})的Region信息！")
    }

    score = it.'Score'
    if(score == null || score == ''){
        throw new Exception("${notice}请补全(${label})的Score信息！")
    }

    if(!isFloat(score)){
        throw new Exception("${notice}请填写(${label})的正确的Score信息！")
    }
    score = score.split('\\.')[0]

    poolRegionRecordLable = deviceName + ":" + wideip + ":" +wideipRecordType + ":" + region + ":" + poolRecordType
    scoreTemp = poolRegionRecordScoreMap.get(poolRegionRecordLable)
    if(scoreTemp == null || scoreTemp == ''){
        poolRegionRecordScoreMap.put(poolRegionRecordLable,score)
    }else{
        if(score != scoreTemp){
            throw new Exception("${notice}请确保(${poolRegionRecordLable})的同一个region中同一记录类型的的Score相同！")
        }
    }

    poolLoadBalancingMode = it.'PoolLoadBalancingMode'
    if(poolLoadBalancingMode == null || poolLoadBalancingMode == ''){
        throw new Exception("${notice}请补全(${poolRegionRecordLable})的PoolLoadBalancingMode信息！")
    }

    if(poolLoadBalancingMode == 'Round_Robin'){
        poolLoadBalancingMode = 'round-robin'
    }else if(poolLoadBalancingMode == 'Ratio'){
        poolLoadBalancingMode = 'ratio'
    }else if(poolLoadBalancingMode == 'Round_Trip_Time'){
        poolLoadBalancingMode = 'lowest-round-trip-time'
    }else {
        throw new Exception("${notice}请选择正确的PoolLoadBalancingMode")
    }

    poolLbModeTemp = poolRegionRecordLbModeMap.get(poolRegionRecordLable)
    if(poolLbModeTemp == null || poolLbModeTemp == ''){
        poolRegionRecordLbModeMap.put(poolRegionRecordLable,poolLoadBalancingMode)
    }else{
        if(poolLoadBalancingMode != poolLbModeTemp){
            throw new Exception("${notice}请确保(${poolRegionRecordLable})的同一个region中同一记录类型的的PoolLoadBalancingMode相同！")
        }
    }

    dataCenter = it.'DataCenter'
    monitor = it.'Monitor'
    xianLu = it.'XianLu'
    destination = it.'Destination'
    port = it.'Port'

    if(poolRecordType == 'a' || poolRecordType == 'aaaa'){
        if(destination == null || destination == ''){
            throw new Exception("${notice}请补全(${poolRegionRecordLable})的Destination信息！")
        }

        if(poolRecordType == 'a' && !isIPAddr(destination)){
            throw new Exception("${notice}请填写(${poolRegionRecordLable})的正确的ipV4地址(xianLu:${xianLu})${destination}")
        }

        if(poolRecordType == 'aaaa' && !isIPV6Addr(destination)){
            throw new Exception("${notice}请填写(${poolRegionRecordLable})的正确的ipV6地址(xianLu:${xianLu})${destination}")
        }

        if(region != 'others') {
            if (ipAddSet.contains(destination)){
                throw new Exception("${notice}(${poolRegionRecordLable})的xianLu:${xianLu}的ip地址(${destination})重复")
            }
            ipAddSet.add(destination)

            if(dataCenter == null || dataCenter == ''){
                throw new Exception("${notice}请补全(${poolRegionRecordLable})的DataCenter信息！")
            }

            if(monitor == null || monitor == ''){
                throw new Exception("${notice}请补全(${poolRegionRecordLable})的Monitor信息！")
            }

            if(xianLu == null || xianLu == ''){
                throw new Exception("${notice}请补全(${poolRegionRecordLable})的XianLu信息！")
            }

            if(port == null || port == ''){
                throw new Exception("${notice}请补全(${poolRegionRecordLable})的Port信息！")
            }

            if(!isFloat(port)){
                throw new Exception("${notice}请填写(${poolRegionRecordLable})的正确的Port信息！")
            }

            port = port.split('\\.')[0]

        }else{
            if (!ipAddSet.contains(destination)){
                throw new Exception("${notice}(${poolRegionRecordLable})的ip地址(${destination})在server中不存在")
            }
            if(otherIpAddSet.contains(destination)){
                throw new Exception("${notice}(${poolRegionRecordLable})的ip地址(${destination})重复")
            }
            otherIpAddSet.add(destination)
        }
    }

    def gtmObject = new GtmObject()
    gtmObject.scriptType = scriptType
    gtmObject.deviceName = deviceName
    gtmObject.wideip = wideip
    gtmObject.wideipLoadBalancingMode = wideipLoadBalancingMode
    gtmObject.wideipRecordType = wideipRecordType
    gtmObject.region = region
    gtmObject.score = score
    gtmObject.poolLoadBalancingMode = poolLoadBalancingMode
    gtmObject.poolRecordType = poolRecordType
    gtmObject.dataCenter = dataCenter
    gtmObject.monitor = monitor
    gtmObject.xianLu = xianLu
    gtmObject.destination = destination
    gtmObject.port = port
    gtmObject.resolvingOtherDomain = resolvingOtherDomain
    gtmObject.poolCnameOrMxDomin = poolCnameOrMxDomin

    List<GtmObject> gtmList = mapGtm.get(label)
    if (gtmList == null || gtmList.size() == 0) {
        gtmList = new ArrayList<GtmObject>();
        gtmList.add(gtmObject)
        mapGtm.put(label, gtmList)
    } else {
        gtmList.add(gtmObject)
        mapGtm.put(label, gtmList)
    }

}

cnameOrMxCli = new StringBuilder()
cnameOrMxOut = new StringBuilder()
mapGtm.each { entry ->
    label = entry.key
    splits = label.split(':')
    deviceName = splits[0]
    wideip = splits[1]
    wideipRecordType = splits[2]
    wideipLoadBalancingMode = wideipToLbModeMap.get(label)
    Map<String,List<GtmObject>> regionMap =  new HashMap<String,List<GtmObject>>()
    Map<String,String> ipToServerMap = new HashMap<String,String>()
    aServerCli = new StringBuilder()
    aServerOut = new StringBuilder()
    aaaaServerCli = new StringBuilder()
    aaaaServerOut = new StringBuilder()
    List<GtmObject> gtmList = entry.value
    gtmList.each { it ->
        region = it.region
        poolRecordType = it.poolRecordType
        poolRegionRecordLable = deviceName+":"+wideip+":"+wideipRecordType+":"+region+":"+poolRecordType
        List<GtmObject> regionList = regionMap.get(poolRegionRecordLable)
        if(regionList == null || regionList.size() == 0){
            regionList = new ArrayList<GtmObject>()
            regionList.add(it)
            regionMap.put(poolRegionRecordLable,regionList)
        }else{
            regionList.add(it)
            regionMap.put(poolRegionRecordLable,regionList)
        }

        if(wideipRecordType == 'a' || wideipRecordType == 'aaaa'){
            if(region != 'others'){
                if(poolRecordType == 'a' || poolRecordType == 'aaaa'){
                    createServer = createGtmServer(it)
                    if(poolRecordType == 'a'){
                        aServerCli << createServer + "\n"
                        aServerOut << deviceName + "," + createServer + "," + "\n"
                    }else if(poolRecordType == 'aaaa'){
                        aaaaServerCli << createServer + "\n"
                        aaaaServerOut << deviceName + "," + createServer + "," + "\n"
                    }
                    serverVsName = "${it.xianLu}_${it.wideip}_server_${poolRecordType}:${it.xianLu}_${it.wideip}_vs_${poolRecordType}"
                    ipToServerMap.put(it.destination,serverVsName)
                }
            }
        }
    }

    Map<String,String> retMap = createGtmPoolAndTop(regionMap,ipToServerMap,poolRegionRecordScoreMap,poolRegionRecordLbModeMap)

    poolNames = retMap.get("poolNames")
    if(wideipRecordType == 'a' || wideipRecordType == 'aaaa'){
        cli << "${label}的${wideipRecordType}记录的命令脚本如下：\n"
        cli << aServerCli
        outPut << aServerOut
        cli << aaaaServerCli
        outPut << aaaaServerOut
        cli << retMap.get("gtmAOrAAAAPool")
        outPut << retMap.get("gtmAOrAAAAPoolOut")
        cli << retMap.get("gtmCnameOrMxPool")
        outPut << retMap.get("gtmCnameOrMxPoolOut")
        cli << retMap.get("aOrAAAATopology")
        outPut << retMap.get("aOrAAAATopologyOut")
        cli << retMap.get("cnameOrMxTopology")
        outPut << retMap.get("cnameOrMxTopologyOut")
        createWideip = "tmsh create gtm wideip ${wideipRecordType} ${wideip} pool-lb-mode ${wideipLoadBalancingMode} pools add {${poolNames}}"
        cli << createWideip + "\n"
        outPut << deviceName + "," + createWideip + "," + "\n"
        cli << "tmsh save sys config\n\n\n"
    }else if(wideipRecordType == 'cname' || wideipRecordType == 'mx'){
        cnameOrMxCli << "${label}的${wideipRecordType}记录的命令脚本如下：\n"
        cnameOrMxCli << retMap.get("gtmCnameOrMxPool")
        cnameOrMxOut << retMap.get("gtmCnameOrMxPoolOut")
        cnameOrMxCli << retMap.get("cnameOrMxTopology")
        cnameOrMxOut << retMap.get("cnameOrMxTopologyOut")
        createWideip = "tmsh create gtm wideip ${wideipRecordType} ${wideip} pool-lb-mode ${wideipLoadBalancingMode} pools add {${poolNames}}"
        cnameOrMxCli << createWideip + "\n"
        cnameOrMxOut << deviceName + "," + createWideip + "," + "\n"
        cnameOrMxCli << "tmsh save sys config\n\n\n"
    }

    wideipToPoolsMap.put(label,poolNames)
}

resolvingOtherDomainMap.each { entry ->
    label = entry.key
    gtm = entry.value
    deviceName = gtm.deviceName
    wideip = gtm.wideip
    wideipRecordType = gtm.wideipRecordType
    wideipLbMode = gtm.wideipLoadBalancingMode
    resolvingOtherDomain = gtm.getResolvingOtherDomain()
    poolNames = wideipToPoolsMap.get(deviceName+":"+resolvingOtherDomain+":"+wideipRecordType)
    if(wideipRecordType == 'a' || wideipRecordType == 'aaaa') {
        cli << "${label}复用其他域名(${resolvingOtherDomain})的地址池的脚本：\n"
        createWideip = "tmsh create gtm wideip ${wideipRecordType} ${wideip} pool-lb-mode ${wideipLbMode} pools add {${poolNames}}"
        cli << createWideip + "\n"
        outPut << deviceName + "," + createWideip + "," + "\n"
        cli << "tmsh save sys config\n\n\n"
    }else if(wideipRecordType == 'cname' || wideipRecordType == 'mx'){
        cnameOrMxCli << "${label}复用其他域名(${resolvingOtherDomain})的地址池的脚本：\n"
        createWideip = "tmsh create gtm wideip ${wideipRecordType} ${wideip} pool-lb-mode ${wideipLbMode} pools add {${poolNames}}"
        cnameOrMxCli << createWideip + "\n"
        cnameOrMxOut << deviceName + "," + createWideip + "," + "\n"
        cnameOrMxCli << "tmsh save sys config\n\n\n"
    }
}

def createGtmPoolAndTop( Map<String,List<GtmObject>> regionMap,Map<String,String> ipToServerMap,HashMap<String,String> poolRegionRecordScoreMap,HashMap<String,String> poolRegionRecordLbModeMap){
    gtmAPool = new StringBuilder()
    gtmAPoolOut = new StringBuilder()
    gtmAAAAPool = new StringBuilder()
    gtmAAAAPoolOut = new StringBuilder()
    gtmCnamePool = new StringBuilder()
    gtmCnamePoolOut = new StringBuilder()
    gtmMxPool = new StringBuilder()
    gtmMxPoolOut = new StringBuilder()
    aTopologyTemp = new StringBuilder()
    aTopologyTempOut = new StringBuilder()
    aaaaTopologyTemp = new StringBuilder()
    aaaaTopologyTempOut = new StringBuilder()
    cnameTopologyTemp = new StringBuilder()
    cnameTopologyTempOut = new StringBuilder()
    mxTopologyTemp = new StringBuilder()
    mxTopologyTempOut = new StringBuilder()
    gtmAOrAAAAPool = new StringBuilder()
    gtmAOrAAAAPoolOut = new StringBuilder()
    aOrAAAATopology = new StringBuilder()
    aOrAAAATopologyOut = new StringBuilder()
    gtmCnameOrMxPool = new StringBuilder()
    gtmCnameOrMxPoolOut = new StringBuilder()
    cnameOrMxTopology = new StringBuilder()
    cnameOrMxTopologyOut = new StringBuilder()
    retPoolNames = new StringBuilder()
    regionMap.each { entry ->
        poolRegionRecordLable = entry.key
        splits = poolRegionRecordLable.split(":")
        deviceName = splits[0]
        wideip = splits[1]
        wideipRecordType = splits[2]
        region = splits[3]
        poolRecordType = splits[4]
        poolLoadBalancingMode = poolRegionRecordLbModeMap.get(poolRegionRecordLable)
        score = poolRegionRecordScoreMap.get(poolRegionRecordLable)
        List<GtmObject> gtmList = entry.value
        poolMembers = new StringBuilder()
        gtmList.each { it ->
            if(poolRecordType == 'a' || poolRecordType == 'aaaa'){
                serverVsName = ipToServerMap.get(it.destination)
                poolMembers << " ${serverVsName}"
            }else if(poolRecordType == 'cname' || poolRecordType == 'mx'){
                poolMembers << " ${it.poolCnameOrMxDomin}"
            }

        }
        poolName = " pool_${region}_${wideip}_${poolRecordType}"
        if(poolRecordType == 'a'){
            pool = "tmsh create gtm pool a ${poolName} ttl 300 load-balancing-mode ${poolLoadBalancingMode} alternate-mode none  fallback-mode round-robin members add {${poolMembers} }"
            gtmAPool << pool + "\n"
            gtmAPoolOut << deviceName + "," + pool + "," + "\n"
            topology = "tmsh create gtm topology ldns: region ${region} server: pool ${poolName} score ${score}"
            aTopologyTemp << topology + "\n"
            aTopologyTempOut << deviceName + "," + topology + "," + "\n"
        }else if(poolRecordType == 'aaaa'){
            pool = "tmsh create gtm pool aaaa ${poolName} ttl 300 load-balancing-mode ${poolLoadBalancingMode} alternate-mode none  fallback-mode round-robin members add {${poolMembers} }"
            gtmAAAAPool << pool + "\n"
            gtmAAAAPoolOut << deviceName + "," + pool + "," + "\n"
            topology = "tmsh create gtm topology ldns: region ${region} server: pool ${poolName} score ${score}"
            aaaaTopologyTemp << topology + "\n"
            aaaaTopologyTempOut << deviceName + "," + topology + "," + "\n"
        } else if(poolRecordType == 'cname'){
            pool = "tmsh create gtm pool cname ${poolName} ttl 300 load-balancing-mode ${poolLoadBalancingMode} alternate-mode none  fallback-mode round-robin members add {${poolMembers} }"
            gtmCnamePool << pool + "\n"
            gtmCnamePoolOut << deviceName + "," + pool + "," + "\n"
            topology = "tmsh create gtm topology ldns: region ${region} server: pool ${poolName} score ${score}"
            cnameTopologyTemp << topology + "\n"
            cnameTopologyTempOut << deviceName + "," + topology + "," + "\n"
        }else if(poolRecordType == 'mx'){
            pool = "tmsh create gtm pool mx ${poolName} ttl 300 load-balancing-mode ${poolLoadBalancingMode} alternate-mode none  fallback-mode round-robin members add {${poolMembers} }"
            gtmMxPool <<  pool + "\n"
            gtmMxPoolOut <<  deviceName + "," + pool + "," + "\n"
            topology = "tmsh create gtm topology ldns: region ${region} server: pool ${poolName} score ${score}"
            mxTopologyTemp << topology + "\n"
            mxTopologyTempOut << deviceName + "," + topology + "," + "\n"
        }
        retPoolNames << "${poolName}"
    }
    gtmAOrAAAAPool << gtmAPool
    gtmAOrAAAAPool << gtmAAAAPool
    gtmAOrAAAAPoolOut << gtmAPoolOut
    gtmAOrAAAAPoolOut << gtmAAAAPoolOut
    aOrAAAATopology << aTopologyTemp
    aOrAAAATopology << aaaaTopologyTemp
    aOrAAAATopologyOut << aTopologyTempOut
    aOrAAAATopologyOut << aaaaTopologyTempOut
    gtmCnameOrMxPool << gtmCnamePool
    gtmCnameOrMxPool << gtmMxPool
    gtmCnameOrMxPoolOut << gtmCnamePoolOut
    gtmCnameOrMxPoolOut << gtmMxPoolOut
    cnameOrMxTopology << cnameTopologyTemp
    cnameOrMxTopology << mxTopologyTemp
    cnameOrMxTopologyOut << cnameTopologyTempOut
    cnameOrMxTopologyOut << mxTopologyTempOut
    Map<String,String> retMap = new HashMap<>()
    retMap.put("poolNames",retPoolNames)
    retMap.put("gtmAOrAAAAPool",gtmAOrAAAAPool)
    retMap.put("gtmAOrAAAAPoolOut",gtmAOrAAAAPoolOut)
    retMap.put("aOrAAAATopology",aOrAAAATopology)
    retMap.put("aOrAAAATopologyOut",aOrAAAATopologyOut)
    retMap.put("gtmCnameOrMxPool",gtmCnameOrMxPool)
    retMap.put("gtmCnameOrMxPoolOut",gtmCnameOrMxPoolOut)
    retMap.put("cnameOrMxTopology",cnameOrMxTopology)
    retMap.put("cnameOrMxTopologyOut",cnameOrMxTopologyOut)
    retMap
}

def createGtmServer(GtmObject gtm){
    gtmServerCli = "tmsh create gtm server ${gtm.xianLu}_${gtm.wideip}_server_${gtm.poolRecordType} addresses add { ${gtm.destination} } product generic-host datacenter ${gtm.dataCenter} monitor ${gtm.monitor} virtual-servers add { ${gtm.xianLu}_${gtm.wideip}_vs_${gtm.poolRecordType} { destination ${gtm.destination}:${gtm.port} } }"
    gtmServerCli
}

def boolean isIPAddr(String addr){
    return addr ==~ /((?:(?:25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d)))\.){3}(?:25[0-5]|2[0-4]\d|((1\d{2})|([1-9]?\d))))/
}

def boolean isIPV6Addr(String addr){
    return addr ==~ /^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/
}

def boolean isFloat(f){
    return f ==~ /^[1-9][0-9]+.?[0-9]*$/
}


def boolean isDomain(String domain){
    return domain ==~ /[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+.?/
}

cli << cnameOrMxCli
outPut << cnameOrMxOut
println cli

gCalendar= new GregorianCalendar()

nowtime = gCalendar.time.toString()

new FileWriter(filepath + 'F5-VS-V12-GTM ' + nowtime.replaceAll(':','-') + '.csv').withWriter { writer ->
    writer.write(outPut.toString()) }

new FileWriter(filepath + 'F5-VS-V12-GTM ' + nowtime.replaceAll(':','-') + '.txt').withWriter { writer ->
    writer.write(cli.toString())
}


println "exceldone"




