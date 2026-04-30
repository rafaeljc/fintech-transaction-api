# Fintech - Programming Challenge

> **Note:** This is a translation of the original challenge. Attribute names (e.g. `amount`, `dateTime`) and endpoint names (e.g. `/transactions`, `/statistics`) were kept as close to the original as possible.
>
> Endpoint names were changed from singular to plural (e.g. `/transaction` → `/transactions`) following REST API best practices.

## 1. Introduction

Your mission, should you choose to accept it, is to create a REST API that receives Transactions and returns Statistics based on those transactions. For this challenge, the API must be created using Java or [Kotlin](https://kotlinlang.org/) and Spring Boot.

A good place to start is [Spring Starter](https://start.spring.io/).

> **Tip:** There is no right or wrong way to solve this challenge! We will evaluate things such as code quality, how easy the code is to understand, project organization, quantity and quality of tests, attention to security, and many other factors :)

## 2. Challenge Definition

In this challenge you must **create a REST API** on [GitHub](https://github.com/) or [GitLab](https://gitlab.com/). **Please read all instructions carefully below!**

### 2.1. Technical Constraints

Your project:

- **MUST** be hosted on [GitHub](https://github.com/) or [GitLab](https://gitlab.com/)
- **MUST NOT** fork any other project
- **MUST** have at least 1 commit for each endpoint (minimum of 3 commits)
    - We want to see the evolution of your project over time ;)
- All commits **MUST** be made by the same user who created the project
    - We understand that some people have personal and professional accounts, or a separate account used for studying. Pay attention to this if you are one of those people!
- **MUST** follow exactly the endpoints described below
    - For example, `/transactions` is not the same as `/transaction`
- **MUST** accept and respond with objects exactly as described below
    - For example, `dateTime` is not the same as `date-time` or `transactionDT`
- **MUST NOT** use any database systems (such as H2, MySQL, PostgreSQL, ...) or cache systems (such as Redis, Memcached, Infinispan, ...)
- **MUST** store all data **in memory**
- **MUST** accept and respond only with [JSON](https://www.json.org/json-en.html)

> **Attention!** For security reasons, we cannot accept projects sent as files. You **MUST** make your project publicly available so that we can access and review it! After receiving a response from us, feel free to make your project **private** :)

### 2.2. API Endpoints

Below are the endpoints that must be present in your API and the expected functionality of each one.

#### 2.2.1. Receive Transactions: `POST /transactions`

This is the endpoint that will receive Transactions. Each transaction consists of a `amount` and a `dateTime` representing when it happened:

```json
{
  "amount": 123.45,
  "dateTime": "2020-08-07T12:34:56.789-03:00"
}
```

The fields in the JSON above mean the following:

| Field      | Meaning                                                        | Required? |
|------------|----------------------------------------------------------------|-----------|
| `amount`   | **Floating-point decimal value** of the transaction            | Yes       |
| `dateTime` | **Date/Time in ISO 8601 format** when the transaction occurred | Yes       |

> **Tip:** Spring Boot, by default, can parse dates in ISO 8601 format without issues. Try using an attribute of type `OffsetDateTime`!

The API will only accept transactions that:

1. Have the fields `amount` and `dateTime` filled in
2. The transaction **MUST NOT** occur in the future
3. The transaction **MUST** have occurred at any time in the past
4. The transaction **MUST NOT** have a negative value
5. The transaction **MUST** have a value equal to or greater than `0` (zero)

As a response, this endpoint is expected to return:

- `201 Created` with no body
    - The transaction was accepted (that is, validated, valid, and stored)
- `422 Unprocessable Entity` with no body
    - The transaction was **not** accepted for any reason (1 or more acceptance criteria were not met — for example: a transaction with a value lower than `0`)
- `400 Bad Request` with no body
    - The API could not understand the client request (for example: invalid JSON)

#### 2.2.2. Clear Transactions: `DELETE /transactions`

This endpoint simply **deletes all stored transaction data**.

As a response, this endpoint is expected to return:

- `200 OK` with no body
    - All information was successfully deleted

#### 2.2.3. Calculate Statistics: `GET /statistics`

This endpoint must return statistics for transactions that **occurred in the last 60 seconds (1 minute)**. The statistics that must be calculated are:

```json
{
  "count": 10,
  "sum": 1234.56,
  "avg": 123.456,
  "min": 12.34,
  "max": 123.56
}
```

The fields in the JSON above mean the following:

| Field   | Meaning                                                   | Required? |
|---------|-----------------------------------------------------------|-----------|
| `count` | **Number of transactions** in the last 60 seconds         | Yes       |
| `sum`   | **Total sum of transacted value** in the last 60 seconds  | Yes       |
| `avg`   | **Average transacted value** in the last 60 seconds       | Yes       |
| `min`   | **Lowest transacted value** in the last 60 seconds        | Yes       |
| `max`   | **Highest transacted value** in the last 60 seconds       | Yes       |

> **Tip:** There is a Java 8+ object called `DoubleSummaryStatistics` that may help you or serve as inspiration.

As a response, this endpoint is expected to return:

- `200 OK` with the statistics data
    - A JSON with the fields `count`, `sum`, `avg`, `min`, and `max` all filled with their respective values
    - **Attention!** When there are no transactions in the last 60 seconds, consider all values as `0` (zero)

## 4. Extras

Below are some extra challenges in case you want to test your knowledge to the fullest! None of these requirements are mandatory, but they are desirable and may be a differentiator!

1. **Automated Tests:** Whether unit or functional, automated tests are important and help prevent future issues. If you create automated tests, pay attention to their effectiveness! For example, testing only the "happy paths" is not very effective.
2. **Containerization:** Can you provide a way to make your application available as a container? *NOTE: It is not necessary to publish your application container!*
3. **Logs:** Does your application report what is happening while it runs? This is useful to help developers troubleshoot possible issues.
4. **Observability:** Does your API have any endpoint for checking application health (healthcheck)?
5. **Performance:** Can you estimate how long your application takes to calculate statistics?
6. **Error Handling:** Spring Boot gives developers tools to improve default error handling. Can you change the default errors to return *which* errors occurred?
7. **API Documentation:** Can you document your API? There are [tools](https://swagger.io/) and [standards](http://raml.org/) that can help with this!
8. **System Documentation:** Your application will probably need to be built before it can run. Can you document how another person seeing your application for the first time can build and run it?
9. **Configuration:** Can you make your application configurable regarding the number of seconds used to calculate statistics? For example: the default is 60 seconds, but what if the user wants 120 seconds?