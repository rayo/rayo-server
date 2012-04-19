package com.rayo.provisioning.model;

import static com.voxeo.utils.Strings.isEmpty;

import java.net.URI;

public class AddressDto {
    private URI href;
    
    private String type;
    private String prefix;
    private String number;
    private String displayNumber;
    private String username;
    private String password;
    private String nickname;
    private String token;
    private String channel;
    private String address;
    private String serviceId;
    
    private String city;
    private String state;
    private String country;

    private URI provider;
    private String providerName;
    
    private Boolean smsEnabled;
    private Integer smsRateLimit;
    private URI application;
    
    // Used for adding addresses to pools
    private Integer providerId;
    private Integer poolId;
    private Integer exchangeId;
    
    @Override
    public String toString() {
        return String.format("[AddressDto %s:%s]",type, getValue());
    }

    public String getValue() {
        String result = null;
        if (!isEmpty(number)) {
            result = number;
        } else if (!isEmpty(token)) {
            result = token;
        } else if (!isEmpty(username)) {
            result = username;
        } else if (!isEmpty(address)) {
            result = address;
        }
        return result;
    }

    public URI getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = (href != null ? URI.create(href) : null);
    }

    public void setHref(URI href) {
        this.href = href;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        if (isEmpty(nickname)) {
            nickname = null;
        }
        this.nickname = nickname;
    }

    public boolean hasNumber() {
        return !isEmpty(number);
    }

    public boolean hasPrefix() {
        return !isEmpty(prefix);
    }

    public boolean hasUsername() {
        return !isEmpty(username);
    }

    public boolean hasType() {
        return !isEmpty(type);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String region) {
        this.country = region;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean hasToken() {
        return !isEmpty(token);
    }

    public URI getApplication() {
        return application;
    }

    public void setApplication(URI application) {
        this.application = application;
    }
    
    public boolean hasApplication() {
        return application != null;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    public void setDisplayNumber(String displayNumber) {
        this.displayNumber = displayNumber;
    }

    public URI getProvider() {
        return provider;
    }

    public void setProvider(URI provider) {
        this.provider = provider;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Integer getSmsRateLimit() {
        return smsRateLimit;
    }

    public void setSmsRateLimit(Integer smsRateLimit) {
        this.smsRateLimit = smsRateLimit;
    }

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public Integer getProviderId() {
		return providerId;
	}

	public void setProviderId(Integer providerId) {
		this.providerId = providerId;
	}

	public Integer getPoolId() {
		return poolId;
	}

	public void setPoolId(Integer poolId) {
		this.poolId = poolId;
	}

	public Integer getExchangeId() {
		return exchangeId;
	}

	public void setExchangeId(Integer exchangeId) {
		this.exchangeId = exchangeId;
	}
}
