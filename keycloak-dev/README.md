## Setup KeyCloak Server
Keycloak is an open source Identity and Access Management solution targeted towards modern applications and services.

### Installing KeyCloak server
Clone the keycloak-dev repository
```shell
git clone git@github.com:brulejr/keycloak-dev.git
```
Start the development service
```shell
cd keybloack-dev
docker compose up -d
```

### Configuring KeyCloak server
Configure the KeyCloak server according to the instructions found [here](https://davidtruxall.com/secure-a-vue-js-app-with-keycloak/).

#### Create Realm
Navigate to the upper left corner to discover the **Add realm** button, 
and click the button to show the **Add realm** page. 

Create a new realm name called `mytasks`, and click the **Create** button.

#### Create Clients
Navigate to the **Clients** page, and create a new public client with the following fields:

| Field     | Value                   |
|:----------|:------------------------|
| Client ID | `mytasks-ui`          |
| Root URL  | `http://localhost:4080` |

Within the client's **Settings** tab, ensure the following fields are configured:

| Field                         | Value                   | Reason                                                  |
|:------------------------------|:------------------------|:--------------------------------------------------------|
| Client Protocol               | openid-connect          | Enables standard OpenID flow                            |
| AccessType                    | public                  | Clients do not require a secret                         |
| Standard Flow Enabled         | True                    | Enables standard OpenID flow                            |
| Direct Access Grants Enabled  | True                    | Allows user / password logon                            |
| Root URL                      | `http://localhost:4080` | Gets prepended to redirected URLS                       |
| Valid Redirect URIs           | `*`                     | Redirect location after logout                          |
| Admin URL                     | `http://localhost:4080` | Provides access to client admin tools                   |
| Web Origins                   | `http://localhost:3080` | Allowed origin for CORS (Really important for web apps) |

Create a second client named `mytasks-ms` with the following settings:

| Field                         | Value                   | Reason                                                  |
|:------------------------------|:------------------------|:--------------------------------------------------------|
| Client Protocol               | openid-connect          | Enables standard OpenID flow                            |
| AccessType                    | bearer-only             | Allows webservers that never initiate a login           |


#### Create Roles
Navigate to the **Roles** page, and create a role with the name of `ROLE_USER`.

Create a second role with the name of `ROLE_ADMIN`. Ensure that this composite role is associated with the `ROLE_USER`
role.


#### Create User
Navigate to the **Users** page, and create a user with the following fields:

| Field          | Value     |
|:---------------|:----------|
| Username       | `test`   |
| First Name     | `Test`    |
| Last Name      | `Test`   |
| Email Verified | *checked* |

Click the **Credentials** tab, and change the password to `test`. 
Be sure to uncheck the **Temporary** flag so that the password does not have to be reset.

Finally, click on the **Role Mappings** tab, and assign the `ROLE_USER` role to this new user.

## Keycloak Endpoints
The following are the important endpoints provided by Keycloak and use the following parameters:
* `server`, the machine running Keycloak (e.g. `http://localhost`)
* `realm`, the authentication realm (e.g. `todo`)

| Name | Endpoint                                                        |
|:-----|:----------------------------------------------------------------|
|OpenID Configuration|`{{server}}/auth/realms/{{realm}}/.well-known/openid-configuration`|
|Authorize|`{{server}}/auth/realms/{{realm}}/protocol/openid-connect/auth?response_type=code&client_id=jwtClient`|
|Token|`{{server}}/auth/realms/{{realm}}/protocol/openid-connect/token`|
|User Information|`{{server}}/auth/realms/{{realm}}/protocol/openid-connect/userinfo`|
|Token Introspect|`{{server}}/auth/realms/{{realm}}/protocol/openid-connect/token/introspect`|

# Resources

### Articles
* [Keycloak with Spring Boot and Kotlin- Introduction](https://codersee.com/keycloak-with-spring-boot-and-kotlin-introduction/)
