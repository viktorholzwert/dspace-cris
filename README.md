# Notify Project Documentation

## ldn-coar-notify.cfg  - Configuration

The notify project includes some limits to communications between hosts, in particular only allowed hosts can reach the ldn-InBox and record status updates about items.

To reach this result, the file **ldn-coar-notify.cfg** under the path **dspace/config/modules** needs to be configured as follow:

- **Setting up the trusted hosts which can reach out the ldn-inBox endpoint**

trusted hosts can be configured in two different ways, both ways can be used at the same time as needed.

​		`ldn-trusted.from.ip =`
​		`ldn-trusted.from.hostname =`

trusted hosts can be set up using their **IP addresses** if they’re static and known or using the **hostnames** which represents the **easiest and most dinamic solution**. (This solution uses the DNS to retrieve dinamically the IP addresses)

Here’s an example of configuration for both properties:

​		`ldn-trusted.from.ip = 192.168.0.32, 192.168.1.58`
​		`ldn-trusted.from.hostname = example.com, ldninbox.antleaf.com/inbox`

This step is mandatory, otherwise no host will be able to perform any operation on the repository

- **Configuring review/endorsment endpoints, IDs, and names**

The next step is configuring review and endorsement IDs, **the same IDs need to be specified in input-form.xml file and must match with these ones**. Note the IDs must match with resource ID path given in the JSON-LD request body (**Without including** `http://` **or** `https://`).

Using:

​		`service.service-id.ldn = service1, ... , service2`

example:

​		`service.service-id.ldn = example.com, ldninbox.antleaf.com/inbox`

we can define IDs for services. **Services listed under** `review.service-id.ldn` **are used for the review  or endorsement process**. To define whether the service provides both review and endorsement we need to set up the following property:

​		`service.<service-id>.endorsement = true`
Here is an example of configuration for this property:

​		`service.example.com.endorsement = true`
​		`service.ldninbox.antleaf.com/inbox.endorsement = false`

Note that “example.com" and “ldninbox.antleaf.com/inbox" are the services IDs.



**For each specified ID we need to set up a Name and an Endpoint**. Both can be set up in the following way:

​		`service.<service-id>.endpoint = https://example.com`
​		`service.<service-id>.endpoint = https://example.com`

​		`service.<service-id>.name = Service Review 1`
​		`service.<service-id>.name = Service Review 2`

Here is an example:

**Set the endpoint** for each review service ID listed above
		`service.example.com.endpoint = https://example.com`
		`service.ldninbox.antleaf.com/inbox.endpoint = https://ldninbox.antleaf.com/inbox`

**Set the services name**
		`service.example.com.name = Example Review Endorsement`
		`service.ldninbox.antleaf.com/inbox.name = ldninbox.antleaf.com`



**the general rule is: Once a new service ID is added, then set up the name, endpoint and whether the service provides endorsement**

​		`service.service-id.ldn = service1, service2, newServiceID`

If we add another review service as shown above named “newServiceID" the following properties must be configured

​		`service.newServiceID.endpoint = https://example.com`

​		`service.newServiceID.name = Service Review And Endorsement`

​		`service.newServiceID.endorsement = true`

## Further Configuration

A few other properties can be configured in the ldn-coar-notify.cfg file.

- `notify.status.details-page.page-size = 10`

with the above property we can choose the **maximum number of items to be displayed in the notify status report page**. This is pre-configured and set to ten.

- `coar.notify.local-inbox-endpoint = ${dspace.baseUrl}/ldn-inbox`

This property is pre-configured as well. **It is used during the review/endorsement request step set in the Origin field of the JSON-LD request**

- `ldn-trusted.localhost.default = true`

Another pre-configured property. While set to true the LDN inBox of the repository **will consider as trusted any connection from localhost to the LDN InBox**.

- `coar.notify.max.attempts.request-review = 3`

A configuration providing the **maximum number of attempts if the request sent to the endpoint gives an error status code**.

- `coar.notify.sleep-between-attempts.request-review = 1000`

A configuration providing the **maximum time in milliseconds to wait between each attempts if the request sent to the endpoint gives an error status code**.

- `coar.notify.timeout.request-review = 5000`

A configuration providing the **timeout time when contacting the a given endpoint**.

## Configuring the Input Form

In the **dspace/config/input-form.xml** a specific metadata used for initializing the notify process is set up. the `coar.notify.initialize` metadata is used to keep track of the initialization process (i.e. to which services we requested the review for an item).

The values shown to the submitter are set up in the `service_type` **value-pairs tag**. Following there’s an example of configuring the `service_type` value list.

```
<value-pairs value-pairs-name="service_type" dc-term="type">
	<pair>
		<displayed-value>Service 1</displayed-value>
		<stored-value>example.com</stored-value>
	</pair>
	<pair>
		<displayed-value>Service 2</displayed-value>
		<stored-value>ldninbox.antleaf.com/inbox</stored-value>
	</pair>
</value-pairs>
```

**As said before, we need to be sure the entries we set in this value-pairs tag have a stored-value that matches a Service ID configured in the ldn-coar-notify.cfg file**. Any unkown or wrongly configured pair might lead to undesired side effects which might affect the software behavior.

As a general rule, for each service which needs to be shown we need to configure add the following element in the value-pairs

```
<pair>
	<displayed-value> ...Displayed Service Name... </displayed-value>
	<stored-value> SERVICE_ID </stored-value>
</pair>
```



## Configuring an Action for a different request type

When receiving a LDN Request the action to execute is decided according a mapping configured on a spring file named ldn-coar-notify.xml under the path dspace/config/spring/api.

the bean: `<bean id="ldnActionsMapping" name="ldnActionsMapping" class="java.util.HashMap">` contains the references to the beans to be executed when a certain type of request is received.

To configure a new type of request with the corresponding action it is needed to add a new entry to the cited `ldnActionsMapping` bean, associating the key representing the request type to a bean referencing a subclass of `org.dspace.ldn.LDNPayloadProcessor`.

Any newly created class to handle a new request must be a subclass of `org.dspace.ldn.LDNPayloadProcessor`  

## Email Group Configuration

The Notify flow can be configured as described in the next paragraph, a further customization is the possibility to send emails for each notify LDN received request.

Emails can be sent to a single email, group or submitter. In order to send emails to a group we need to configure email groups in the ldn-coar-notify.cfg file

to create a new group we need to add a new entry to the file as shown:

​		`email.<group_name>.list = email1@email.com, email2@email.com, ... , emailn@email.com`

The choosen group name will be used when configuring an email action in the next paragraph.

Example of mapping a request type to a set of Action



```
...
<bean id="ldnActionsMapping" name="ldnActionsMapping"class="java.util.HashMap">
	<constructor-arg>
		<map key-type="java.util.Set"
			value-type="org.dspace.ldn.LDNPayloadProcessor">
			<entry>
				<key>
					<set>
						<value>Announce</value>
						<value>coar-notify:ReviewAction</value>
					</set>
				</key>
				<ref bean="announceReviewAction" />
			</entry>

			...

			<entry>
				<key>
					<set>
						<value>Reject</value>
						<value>coar-notify:ReviewAction</value>
					</set>
				</key>
				<ref bean="rejectReviewAction" />
			</entry>
		</map>
	</constructor-arg>

</bean>


<bean name="announceReviewAction"
	class="org.dspace.ldn.LDNAnnounceReviewAction">
	<property name="ldnActions">
		<list value-type="org.dspace.ldn.LDNAction">
			<bean name="sendEmail" class="org.dspace.ldn.LDNEmailAction">
				<property name="actionSendFilter"
					value="stefano.maffei@4science.com" />
				<property name="actionSendEmailTextFile"
					value="coar_notify_reviewed" />
			</bean>
		</list>
	</property>
</bean>
....
<bean name="rejectReviewAction"
	class="org.dspace.ldn.LDNRejectReviewAction">
	<property name="ldnActions">
		<list value-type="org.dspace.ldn.LDNAction">
			<bean name="sendEmail" class="org.dspace.ldn.LDNEmailAction">
				<property name="actionSendFilter"
					value="GROUP:admin" />
				<property name="actionSendEmailTextFile"
					value="coar_notify_rejected" />
			</bean>
		</list>
	</property>
</bean>
...
```

Note that the **key is defined over a set of values** because the request type uses two different Vocabularies to define a type. Check here and here to check the sets of the different types.

[Notify Set](https://notify.coar-repositories.org/schema/notify.json)
[Activity Streams Set](https://www.w3.org/TR/activitystreams-vocabulary/#h-activity-types)



### Further Actions set up

As shown in the above snippet code each bean mapped to a Notify action has a property called ldnActions with this propery we can define one or more or even no action to execute after each Notify  basic task (setting up metadata, deleting metadata) is completed.

ldnActions is defined as a list of beans subclasses of org.dspace.ldn.LDNAction. We can define our own task by creating a new subclass of LDNAction and implementing the method:

​		`public abstract ActionStatus executeAction(Context context, NotifyLDNDTO ldnRequestDTO);`

The method executeAction returns a org.dspace.ldn.ActionStatus value which are basically CONTINUE and ABORT allowing the possibility to continue or stop with the execution of the next task if a certain condition is verified.

## Sending Emails

Sending emails is a pre-configured task. The class org.dspace.ldn.LDNEmailAction provides an implementation for sending emails to specified recipients. To configure this action, a bean of LDNEmailAction must be configured as follow:

```
<bean name="sendEmail" class="org.dspace.ldn.LDNEmailAction">
	<property name="actionSendFilter" value="GROUP:admin" />
	<property name="actionSendEmailTextFile" value="coar_notify_rejected" />
</bean>
```


Where `actionSendEmailTextFile` is the email text file containing the email text to be sent and `actionSendFilter` is the parameter used to define recipients.



`actionSendFilter` can have the following values:

- `<single email>`
- `GROUP:<group_name>`
- `SUBMITTER`

SUBMITTER allows sending the email to the EPerson who submitted the Item in the repository.



**There are four pre-configured email messages** that we can use to configure the property `actionSendEmailTextFile`:

- `coar_notify_accepted`

- `coar_notify_endorsed`
- `coar_notify_rejected`

- `coar_notify_reviewed`

## Installation

1. Clone the git repository (`git clone git://github.com/DSpace/DSpace.git`)
2. Checkout the branch (`git checkout coar-notify-5`)
3. Configure your **local.cfg** file with the proper configuration
4. Run the following command: `mvn clean -U package` from the project parent folder
5. Run `cd dspace/target/dspace-installer` and the `sudo ant fresh_install` if it is the first time you're installing DSpace otherwise run `sudo ant update`

## Notes

- The metadata list used for the notify project is listed unded the **coar-types.xml** file under the path **dspace/config/registries/**

- Documentation for Linked Data Notification [here](https://www.w3.org/TR/ldn/)

- Notify example scenarios [here](https://notify.coar-repositories.org/scenarios/)
