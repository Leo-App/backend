# backend
LeoApp backend services, currently consisting of:

## LeoApp REST API

This API provides a RESTful way to interact with the LeoApp database and thus improves on the old system. Currently supported features are user related interactions,
news and surveys.

### Authentication

To successfully use the API, you must include the **authentication header** in your HTTP-requests. The value is based on the verification checksum of a specific user and
has to be calculated each time prior to usage (see [GitHub](https://github.com/Leo-App/android/blob/5aa08afd84f5113b929aab7e299766203459244c/app/src/main/java/de/slgdev/leoapp/utility/Utils.java#L392-L414)).

An exception to this verification is the "add user"-endpoint where the authentication happens over the verification checksum.
### Errors
Errors follow the syntax

```json
{
    "error": {
        "code": 400,
        "message": "some error"
    }
}
```

The error codes correspond to the HTTP status codes.

