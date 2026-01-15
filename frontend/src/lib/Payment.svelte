<script>
  import { sendWsMessage, messages } from '../socket.svelte.js';
  import { sessionToken } from '../state.svelte.js';
  import { onMount } from 'svelte';

  let amount = "9.99";
  let description = 'Test payment';
  let paymentType = 'single';
  let isSatsSelected = false;
  let currency = 'EUR';
  let paymentsHistory = [];

  onMount(() => {
    sendWsMessage('RequestPaymentsHistory,' + $sessionToken);
  });

  function sendPayment() {
    if (isSatsSelected) {
      sendWsMessage('RequestSinglePayment,' + $sessionToken + ',Millisats,' + (amount + '000') + ',' + description + ',' + paymentType);
    } else {
      sendWsMessage('RequestSinglePayment,' + $sessionToken + ',' + currency + ',' + amount + ',' + description + ',' + paymentType);
    }
  }

  $: if (!isSatsSelected) {
    if(!amount.includes('.')) {
      amount = amount + '.00';
    }
  }

  $: if (isSatsSelected) {
    if(amount.includes('.')) {
      amount = amount.slice(0, -3);
    }
  }

  $: if ($messages.length > 0) {
    let lastMessage = $messages[$messages.length - 1];
    if (lastMessage.cmd === 'PaymentsHistory') {
      console.log('PaymentsHistory', lastMessage.history);
      paymentsHistory = lastMessage.history;
    }
  }

  function formatPaymentStatus(payment) {
    if (payment.paid === undefined || payment.paid === null) {
      return 'Pending';
    }
    return payment.paid ? 'Paid' : 'Not Paid';
  }

  function formatPaymentStatusClass(payment) {
    if (payment.paid === undefined || payment.paid === null) {
      return '';
    }
    return payment.paid ? 'uk-badge-secondary' : 'uk-badge-destructive';
  }
</script>

<div class="space-y-6">
  <div>
    <h3 class="text-lg font-medium">Payment</h3>
    <p class="text-muted-foreground text-sm">
      Request a single payment.
    </p>
  </div>
  <div class="border-border border-t"></div>
 
  <div class="space-y-2">
    <label class="uk-form-label" for="username">Currency</label>
    <div class="flex items-center space-x-2">
      <input
        class="uk-toggle-switch uk-toggle-switch-primary"
        id="toggle-switch"
        type="checkbox"
        bind:checked={isSatsSelected}
      />
      <label class="uk-form-label" for="toggle-switch">Sats</label>
    </div>
    {#if !isSatsSelected}
      <input class="uk-input w-24" type="text" placeholder="EUR" bind:value={currency} />
    {/if}
    <div class="uk-form-help text-muted-foreground">
      Select the currency you want to pay in.
    </div>
  </div>

  <div class="space-y-2">
    <label class="uk-form-label" for="username">Amount</label>
    <input
      class="uk-input"
      id="amount"
      type="text"
      placeholder="100"
      bind:value={amount}
    />
    <div class="uk-form-help text-muted-foreground">
      Enter the amount you want to pay.
    </div>
  </div>
  <div class="space-y-2">
    <label class="uk-form-label" for="description">Description</label>
    <textarea
      class="uk-textarea"
      id="description"
      placeholder="Test payment"
      bind:value={description}
    ></textarea>
    <div class="uk-form-help text-muted-foreground">
      Enter a description for the payment.
    </div>
  </div>
  <div class="">
    <button class="uk-btn uk-btn-primary" on:click={sendPayment}>Send Payment Request</button>
  </div>
</div>

<div class="space-y-6 mt-8">
  <div>
    <h3 class="text-lg font-medium">Payment History</h3>
    <p class="text-muted-foreground text-sm">
      This is a list of your payment history.
    </p>
  </div>
  <div class="border-border border-t"></div>
  <div class="overflow-x-auto">
    <table class="uk-table uk-table-divider uk-table-small">
      <thead>
        <tr>
          <th>Currency</th>
          <th>Amount</th>
          <th>Description</th>
          <th>Status</th>
          <th>Created At</th>
        </tr>
      </thead>
      <tbody>
        {#each paymentsHistory as payment}
          <tr>
            <td>{payment.currency === 'Millisats' ? 'Sats' : payment.currency}</td>
            <td>{payment.currency === 'Millisats' ? payment.amount / 1000 : payment.amount / 100}</td>
            <td>{payment.description}</td>
            <td><span class="uk-badge {formatPaymentStatusClass(payment)}">{formatPaymentStatus(payment)}</span></td>
            <td>{payment.createdAt}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>
