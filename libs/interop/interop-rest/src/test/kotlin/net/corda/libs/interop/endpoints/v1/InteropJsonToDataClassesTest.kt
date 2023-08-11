package net.corda.libs.interop.endpoints.v1

import net.corda.libs.interop.endpoints.v1.types.CreateInteropIdentityRest
import net.corda.rest.json.serialization.jacksonObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class InteropJsonToDataClassesTest {
    val sampleRequestJson = """
{ "applicationName": "Bob2",
  "groupPolicy": {
    "fileFormatVersion": 1,
    "groupId": "3dfc0aae-be7c-44c2-aa4f-4d0d7145cf08",
    "p2pParameters": {
      "sessionTrustRoots": [],
      "tlsTrustRoots": [
        "default_cert_1",
        "default_cert_2"
      ],
      "sessionPki": "NoPKI",
      "tlsPki": "Standard",
      "tlsVersion": "1.3",
      "protocolMode": "Authenticated_Encryption",
      "tlsType": "OneWay"
    }
  },
  "members": [
    {
      "x500Name": "CN=Member1,OU=Org1,O=Company,L=Location,C=US",
      "owningIdentityShortHash": "hash1",
      "endpointUrl": "http://member1.url",
      "endpointProtocol": "HTTP",
      "facadeIds": ["facade1", "facade2"]
    },
    {
      "x500Name": "CN=Member2,OU=Org2,O=Company,L=Location,C=US",
      "owningIdentityShortHash": "hash2",
      "endpointUrl": "http://member2.url",
      "endpointProtocol": "HTTP",
      "facadeIds": ["facade3", "facade4"]
    }
  ]
}
"""
    @Test
    fun `test group policy as data classes`() {
        val mapper = jacksonObjectMapper() //I suspect this is what Rest server uses to transform JSOn to data classes

        val dataClass: CreateInteropIdentityRest.Request = mapper.readValue(sampleRequestJson,
            CreateInteropIdentityRest.Request::class.java)

        val groupPolicy = dataClass.groupPolicy
        val newGroupPolicyAsString = mapper.writeValueAsString(groupPolicy)

        val originalGroupPolicy = mapper.readTree(sampleRequestJson).get("groupPolicy")
        val newGroupPolicy = mapper.readTree(newGroupPolicyAsString)

        Assertions.assertEquals(originalGroupPolicy, newGroupPolicy)

        println(newGroupPolicyAsString) //TODO remove println
    }
}