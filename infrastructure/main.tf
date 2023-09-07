terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.40.0"
    }
  }
}

provider "azurerm" {
  features {}
}

locals {
  default_name = "${var.product}-${var.component}"
  vault_name   = "${var.product}-${var.env}"
  simple_env   = replace(var.env, "idam-", "")
  rpe_env      = (local.simple_env == "preview") ? "aat" : local.simple_env
  environments = {
    "idam-prod"     = "production",
    "idam-aat"      = "staging",
    "idam-perftest" = "testing",
    "idam-preview"  = "development"
  }
  tags = merge(
    var.common_tags,
    {
    "environment" = lookup(local.environments, var.env, replace(var.env, "idam-", ""))
    },
  )
}

data "azurerm_key_vault" "idam_vault" {
  name                = local.vault_name
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${local.rpe_env}"
  resource_group_name = "rpe-service-auth-provider-${local.rpe_env}"
}

data "azurerm_key_vault_secret" "source_s2s_ref_for_idam_bridge" {
  name         = "microservicekey-idam-user-profile-bridge"
  key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}

resource "azurerm_key_vault_secret" "target_s2s_ref_for_idam_bridge" {
  name         = "idam-user-profile-bridge-s2s-secret"
  value        = data.azurerm_key_vault_secret.source_s2s_ref_for_idam_bridge.value
  key_vault_id = data.azurerm_key_vault.idam_vault.id
}
