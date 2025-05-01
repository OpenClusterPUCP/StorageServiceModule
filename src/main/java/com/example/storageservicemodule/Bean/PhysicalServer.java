package com.example.storageservicemodule.Bean;

import jakarta.persistence.*;

@Entity
@Table(name = "physical_server", schema = "cloud_v3")
public class PhysicalServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, length = 45)
    private String name;

    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "data_ip", nullable = false, length = 45)
    private String dataIp;

    @Column(name = "total_vcpu", nullable = false)
    private Integer totalVcpu;

    @Column(name = "total_disk", nullable = false)
    private Integer totalDisk;

    @Column(name = "total_ram", nullable = false)
    private Integer totalRam;

    @Column(name = "used_ram", nullable = false)
    private Integer usedRam;

    @Column(name = "used_disk", nullable = false)
    private Integer usedDisk;

    @Column(name = "used_vcpu", nullable = false)
    private Integer usedVcpu;

    @Column(name = "status", nullable = false, length = 45)
    private String status;

    @Column(name = "infrastructure_type", nullable = false, length = 45)
    private String infrastructureType;

    @Column(name = "auth_method", nullable = false, length = 45)
    private String authMethod;

    @Column(name = "ssh_port", nullable = false)
    private Integer sshPort;

    @Column(name = "ssh_username", nullable = false, length = 45)
    private String sshUsername;

    @Column(name = "ssh_password", nullable = false, length = 250)
    private String sshPassword;

    @Column(name = "ssh_key_path")
    private String sshKeyPath;

    @Column(name = "gateway_access_ip", length = 45)
    private String gatewayAccessIp;

    @Column(name = "gateway_access_port")
    private Integer gatewayAccessPort;

    @Column(name = "switch_name", nullable = false, length = 45)
    private String switchName;

    @Lob
    @Column(name = "server_type", nullable = false)
    private String serverType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDataIp() {
        return dataIp;
    }

    public void setDataIp(String dataIp) {
        this.dataIp = dataIp;
    }

    public Integer getTotalVcpu() {
        return totalVcpu;
    }

    public void setTotalVcpu(Integer totalVcpu) {
        this.totalVcpu = totalVcpu;
    }

    public Integer getTotalDisk() {
        return totalDisk;
    }

    public void setTotalDisk(Integer totalDisk) {
        this.totalDisk = totalDisk;
    }

    public Integer getTotalRam() {
        return totalRam;
    }

    public void setTotalRam(Integer totalRam) {
        this.totalRam = totalRam;
    }

    public Integer getUsedRam() {
        return usedRam;
    }

    public void setUsedRam(Integer usedRam) {
        this.usedRam = usedRam;
    }

    public Integer getUsedDisk() {
        return usedDisk;
    }

    public void setUsedDisk(Integer usedDisk) {
        this.usedDisk = usedDisk;
    }

    public Integer getUsedVcpu() {
        return usedVcpu;
    }

    public void setUsedVcpu(Integer usedVcpu) {
        this.usedVcpu = usedVcpu;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInfrastructureType() {
        return infrastructureType;
    }

    public void setInfrastructureType(String infrastructureType) {
        this.infrastructureType = infrastructureType;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public void setSshKeyPath(String sshKeyPath) {
        this.sshKeyPath = sshKeyPath;
    }

    public String getGatewayAccessIp() {
        return gatewayAccessIp;
    }

    public void setGatewayAccessIp(String gatewayAccessIp) {
        this.gatewayAccessIp = gatewayAccessIp;
    }

    public Integer getGatewayAccessPort() {
        return gatewayAccessPort;
    }

    public void setGatewayAccessPort(Integer gatewayAccessPort) {
        this.gatewayAccessPort = gatewayAccessPort;
    }

    public String getSwitchName() {
        return switchName;
    }

    public void setSwitchName(String switchName) {
        this.switchName = switchName;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

}